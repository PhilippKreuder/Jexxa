package io.jexxa.infrastructure.drivingadapter.rest;

import static io.jexxa.infrastructure.drivingadapter.rest.RESTfulRPCConvention.createRPCConvention;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.plugin.json.JavalinJson;
import io.jexxa.infrastructure.drivingadapter.IDrivingAdapter;
import io.jexxa.infrastructure.drivingadapter.rest.openapi.OpenAPIConvention;
import io.jexxa.utils.JexxaLogger;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;


public class RESTfulRPCAdapter implements IDrivingAdapter
{
    public static final String HOST_PROPERTY = "io.jexxa.rest.host";
    public static final String HTTP_PORT_PROPERTY = "io.jexxa.rest.port";
    public static final String HTTPS_PORT_PROPERTY = "io.jexxa.rest.https_port";
    public static final String KEYSTORE = "io.jexxa.rest.keystore";
    public static final String KEYSTORE_PASSWORD = "io.jexxa.rest.keystore_password";
    public static final String OPEN_API_PATH = "io.jexxa.rest.open_api_path";

    private static final Gson GSON = getGsonBuilder().create();

    private final Properties properties;
    private Javalin javalin;
    private Server server;
    private ServerConnector sslConnector;
    private ServerConnector httpConnector;
    private OpenAPIConvention openAPIConvention;

    private static final Map<Properties, RESTfulRPCAdapter> rpcAdapterMap = new HashMap<>();

    private RESTfulRPCAdapter(Properties properties)
    {
        this.properties = properties;

        Validate.isTrue(isHTTPEnabled() || isHTTPSEnabled(), "Neither HTTP (" + HTTP_PORT_PROPERTY + ") nor HTTPS (" + HTTPS_PORT_PROPERTY + ") is enabled!");

        if ( isHTTPSEnabled() )
        {
            Validate.isTrue( properties.containsKey( KEYSTORE ), "You need to define a location for keystore ("+ KEYSTORE+ ")");
            Validate.isTrue( properties.containsKey( KEYSTORE_PASSWORD ) , "You need to define a location for keystore-password ("+ KEYSTORE_PASSWORD+ ")");
        }

        setupJavalin();

        registerExceptionHandler();
    }

    public static RESTfulRPCAdapter createAdapter(Properties properties)
    {
        if ( rpcAdapterMap.containsKey(properties) )
        {
            JexxaLogger.getLogger(RESTfulRPCAdapter.class).warn("Tried to create an RESTfulRPCAdapter with same properties twice! Return already instantiated adapter.");
        } else {
            rpcAdapterMap.put(properties, new RESTfulRPCAdapter(properties));
        }

        return rpcAdapterMap.get(properties);
    }

    public void register(Object object)
    {
        Validate.notNull(object);
        registerGETMethods(object);
        registerPOSTMethods(object);
    }


    @Override
    public void start()
    {
        try
        {
            javalin.start();

            if (httpConnector != null ) {
                openAPIConvention.getPath().ifPresent(path -> Javalin.log.info("OpenAPI documentation available at: {}"
                        , "http://" + httpConnector.getHost() + ":" + httpConnector.getPort() +  path ) );
            }
            if (sslConnector != null ) {
                openAPIConvention.getPath().ifPresent(path -> Javalin.log.info("OpenAPI documentation available at: {}"
                        , "http://" + sslConnector.getHost() + ":" + sslConnector.getPort() + path ) );
            }
        } catch (RuntimeException e)
        {
            if (e.getMessage().contains("Port already in use.")) // Javalin states its default port of the server. Therefore we correct the error message here"
            {
                throw new IllegalStateException(
                        RESTfulRPCAdapter.class.getSimpleName()
                        + ": "
                        + e.getCause().getMessage()
                        + ". Please check that IP address is correct and port is not in use."
                );
            }
            throw e;
        }
    }

    @Override
    public void stop()
    {
        rpcAdapterMap.remove(properties);

        javalin.stop();
        Optional.ofNullable(httpConnector).ifPresent(ServerConnector::close);
        Optional.ofNullable(sslConnector).ifPresent(ServerConnector::close);
    }

    @SuppressWarnings("unused")
    public int getHTTPSPort()
    {
        if (sslConnector != null)
        {
            return sslConnector.getLocalPort();
        }

        return getHTTPSPortFromProperties();
    }

    public int getHTTPPort()
    {
        if (httpConnector != null)
        {
            return httpConnector.getLocalPort();
        }

        return getHTTPPortFromProperties();
    }

    boolean isHTTPEnabled()
    {
        return properties.containsKey(HTTP_PORT_PROPERTY);
    }

    boolean isHTTPSEnabled()
    {
        return properties.containsKey(HTTPS_PORT_PROPERTY);
    }

    String getHostname()
    {
        return properties.getProperty(HOST_PROPERTY, "0.0.0.0");
    }

    String getKeystore()
    {
        return properties.getProperty(KEYSTORE, "");
    }

    String getKeystorePassword()
    {
        return properties.getProperty(KEYSTORE_PASSWORD, "");
    }


    private int getHTTPPortFromProperties()
    {
        return Integer.parseInt(properties.getProperty(HTTP_PORT_PROPERTY, "0"));
    }

    private int getHTTPSPortFromProperties()
    {
        return Integer.parseInt(properties.getProperty(HTTPS_PORT_PROPERTY, "0"));
    }

    /**
     * Mapping of exception is done as follows
     * <pre>
     * {@code
     *   {
     *     "Exception": "<exception as json>",
     *     "ExceptionType": "<Type of the exception>",
     *     "ApplicationType": "application/json"
     *   }
     * }
     * </pre>
     *
     */
    private void registerExceptionHandler()
    {
        //Exception Handler for thrown Exception from methods
        javalin.exception(InvocationTargetException.class, (e, ctx) -> {
            var targetException = e.getTargetException();
            targetException.getStackTrace(); // Ensures that stack trace is filled in

            JsonObject exceptionWrapper = new JsonObject();
            exceptionWrapper.addProperty("ExceptionType", targetException.getClass().getName());
            exceptionWrapper.addProperty("Exception", GSON.toJson(targetException));
            exceptionWrapper.addProperty("ApplicationType", GSON.toJson("application/json"));

            ctx.result(exceptionWrapper.toString());
            ctx.status(400);
        });
    }

    private void registerGETMethods(Object object)
    {
        var getCommands = createRPCConvention(object).getGETCommands();

        getCommands.forEach(
                method -> javalin.get(
                        method.getResourcePath(),
                        httpCtx -> invokeMethod(object, method, httpCtx)
                )
        );

        getCommands.forEach( method -> openAPIConvention.documentGET(method.getMethod(), method.getResourcePath()));
    }

    private void registerPOSTMethods(Object object)
    {
        var postCommands = createRPCConvention(object).getPOSTCommands();

        postCommands.forEach(
                method -> javalin.post(
                        method.getResourcePath(),
                        httpCtx -> invokeMethod(object, method, httpCtx)
                )
        );

        postCommands.forEach( method -> openAPIConvention.documentPOST(method.getMethod(), method.getResourcePath()));
    }



    private void invokeMethod(Object object, RESTfulRPCConvention.RESTfulRPCMethod method, Context httpContext ) throws InvocationTargetException, IllegalAccessException
    {
        Object[] methodParameters = deserializeParameters(httpContext.body(), method.getMethod());

        var result = Optional.ofNullable(
                IDrivingAdapter
                        .acquireLock()
                        .invoke(method.getMethod(), object, methodParameters)
        );

        //At the moment we do not handle any credentials
        httpContext.header("Access-Control-Allow-Origin", "*");
        httpContext.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");

        result.ifPresent(httpContext::json);
    }

    private Object[] deserializeParameters(String jsonString, Method method)
    {
        if (jsonString == null ||
                jsonString.isEmpty() ||
                method.getParameterCount() == 0)
        {
            return new Object[]{};
        }

        JsonElement jsonElement = JsonParser.parseString(jsonString);

        // In case we have more than one attribute, we assume a JSonArray
        if ( method.getParameterCount() > 1)
        {
            if ( !jsonElement.isJsonArray() )
            {
                throw new IllegalArgumentException("Multiple method attributes musst be passed inside a JSonArray");
            }
            return readArray(jsonElement.getAsJsonArray(), method);
        }
        else
        {
            Object[] result = new Object[1];
            result[0] = GSON.fromJson(jsonString, method.getParameterTypes()[0]);
            return result;
        }
    }

    private Object[] readArray(JsonArray jsonArray, Method method)
    {
        if (jsonArray.size() != method.getParameterCount())
        {
            throw new IllegalArgumentException("Invalid Number of parameters for method " + method.getName());
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] paramArray = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; ++i)
        {
            paramArray[i] = GSON.fromJson(jsonArray.get(i), parameterTypes[i]);
        }

        return paramArray;
    }

    @SuppressWarnings("NullableProblems") // setToJsonMapper(GSON::toJson) causes this warning because toJson is not annotated
    private void setupJavalin()
    {
        JavalinJson.setFromJsonMapper(GSON::fromJson);
        JavalinJson.setToJsonMapper(GSON::toJson);

        this.javalin = Javalin.create(this::getJavalinConfig);
    }

    private void getJavalinConfig(JavalinConfig javalinConfig)
    {
        javalinConfig.server(this::getServer);
        javalinConfig.showJavalinBanner = false;

        this.openAPIConvention = new OpenAPIConvention(properties, javalinConfig );
    }

    private Server getServer()
    {
        if ( server == null )
        {
            server = new Server();
            if (isHTTPEnabled())
            {
                httpConnector = new ServerConnector(server);
                httpConnector.setHost(getHostname());
                httpConnector.setPort(getHTTPPortFromProperties());
                server.addConnector(httpConnector);
            }

            if (isHTTPSEnabled())
            {
                sslConnector = new ServerConnector(server, getSslContextFactory());
                sslConnector.setHost(getHostname());
                sslConnector.setPort(getHTTPSPortFromProperties());
                server.addConnector(sslConnector);
            }
        }

        return server;
    }

    private SslContextFactory getSslContextFactory() {
        var sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath(RESTfulRPCAdapter.class.getResource("/"+ getKeystore() ).toExternalForm());
        sslContextFactory.setKeyStorePassword(getKeystorePassword());
        return sslContextFactory;
    }

    private static GsonBuilder getGsonBuilder()
    {
        var gsonBuilder = new GsonBuilder();
        registerTypeAdapter(gsonBuilder);
        return gsonBuilder;
    }

    private static void registerTypeAdapter(GsonBuilder gsonBuilder)
    {
        gsonBuilder.registerTypeAdapter(LocalDate.class,
            (JsonDeserializer<LocalDate>) (json, type, jsonDeserializationContext) -> {
                if (json.isJsonPrimitive())
                {
                    return LocalDate.parse(json.getAsJsonPrimitive().getAsString());
                }
                return LocalDate.of(json.getAsJsonObject().get("year").getAsInt(), json.getAsJsonObject().get("month").getAsInt(), json.getAsJsonObject().get("day").getAsInt());
            });

        gsonBuilder.registerTypeAdapter(LocalDate.class,
                (JsonSerializer<LocalDate>) (src, typeOfSrc, serializationContext) -> new JsonPrimitive(src.toString()));


        gsonBuilder.registerTypeAdapter(LocalDateTime.class,
                (JsonDeserializer<LocalDateTime>) (json, type, jsonDeserializationContext) -> LocalDateTime.parse(json.getAsJsonPrimitive().getAsString()));

        gsonBuilder.registerTypeAdapter(LocalDateTime.class,
                (JsonSerializer<LocalDateTime>) (src, typeOfSrc, serializationContext) -> new JsonPrimitive(src.toString()));

        gsonBuilder.registerTypeAdapter(ZonedDateTime.class,
                (JsonDeserializer<ZonedDateTime>) (json, type, jsonDeserializationContext) -> ZonedDateTime.parse(json.getAsJsonPrimitive().getAsString()));

        gsonBuilder.registerTypeAdapter(ZonedDateTime.class,
                (JsonSerializer<ZonedDateTime>) (src, typeOfSrc, serializationContext) ->

             new JsonPrimitive( DateTimeFormatter.ISO_OFFSET_DATE_TIME.
                    format(src.withZoneSameInstant(src.getZone())) ));

    }
}