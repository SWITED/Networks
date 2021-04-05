package com.company;

import java.nio.charset.Charset;

public class Constants
{
    public static final Charset CHARSET = Charset.forName("UTF-8");//кладем в пакет побайтово с помощью этой кодировки
    public static final long MULTICAST_DELAY = (long) (10 * Math.pow(10,3));
    public static final int PORT = 12344;
    public static final String MESSAGE = "Are you here my group bor";
    public static final long LIFE_TIMEOUT = (long) (12 * Math.pow(10,3));

}
