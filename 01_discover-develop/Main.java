package com.company;

public class Main {
    public static void main(String[] args) {
        if(args.length < 2)
        {
            System.out.println("Usage: <multicast-IPv4/multicast-IPv6 PORT>");
            System.exit(1);
        }
        CopyFinder copyFinder = new CopyFinder(args[0],Integer.parseInt(args[1]));
        copyFinder.start();
    }
}
