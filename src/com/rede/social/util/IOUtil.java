package com.rede.social.util;

import java.util.Scanner;

public class IOUtil {

    private static Scanner in = new Scanner(System.in, "UTF-8").useDelimiter("\n");

    public IOUtil() {}

    public String getText(String message) {
        System.out.print(message);
        String result = in.next().trim();
        return result;
    }

    public int getInt(String message) {
        System.out.print(message);
        int number = in.nextInt();
        return number;
    }

    public void showError(String error) {
        System.out.println(error);
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void clearScreen() {
        System.out.print("Pressione <Enter> para continuar...");
        in.next();
        showMessage("\n".repeat(20));
    }

    public void closeScanner() {
        in.close();
    }
}
