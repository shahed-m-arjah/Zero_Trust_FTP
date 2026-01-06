/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.netprogproj;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 *
 * @author C-ROAD
 */

public class PassHasher{
        
         public static String PassHasher(String password){
             try {
                 MessageDigest digest = MessageDigest.getInstance("SHA-256");
                 byte[] hash = digest.digest(password.getBytes());
                 return HexFormat.of().formatHex(hash);  // Java 17+ for hex formatting
                } 
             catch (NoSuchAlgorithmException e) {
                 throw new RuntimeException("SHA-256 not available", e);
                }
             
         }
         public static void main(String[] args) {
             
             //user1 -> password
             //admin -> PASSWORD
             //test -> test
             String pass = "test";
             System.out.println(PassHasher.PassHasher(pass));
    }
    }