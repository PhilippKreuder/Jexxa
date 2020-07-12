package io.jexxa.tutorials.bookstore.domain.domainevent;

import io.jexxa.tutorials.bookstore.domain.valueobject.ISBN13;
import io.jexxa.tutorials.bookstore.domain.valueobject.StoreAddress;

public class BookSoldOut
{
    private final StoreAddress storeAddress;
    private final ISBN13 isbn13;

    public BookSoldOut(StoreAddress storeAddress, ISBN13 isbn13)
    {
        this.storeAddress = storeAddress;
        this.isbn13 = isbn13;
    }

    public ISBN13 getISBN13()
    {
        return isbn13;
    }

    public StoreAddress getStoreAddress()
    {
        return storeAddress;
    }
}
