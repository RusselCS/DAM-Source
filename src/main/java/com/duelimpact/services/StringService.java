package com.duelimpact.services;

import java.util.Arrays;

public class StringService {
	public static boolean endsWithAny(String in, String[] check) {
		boolean ret = false;
		
		in = in.toLowerCase();
		for(int i = 0; i < check.length; i++) {
			ret = ret || in.endsWith(check[i].toLowerCase());
		}
		
		return ret;
	}
	
	public static boolean contains(String[] args, String search) {
		if(args == null || args.length == 0 || search == null) return false;
		
		final String key = search.toLowerCase();
		return Arrays.stream(args).filter(cur -> cur.toLowerCase().equals(key)).count() > 0;
	}
	
	public static void printAll(String[] items) {
		for(String str: items) {
			System.out.println(str);
		}
	}
	
	public static String[] appendAllPre(String app, String[] origin) {
		if(app == null || origin == null) return null;
		
		for(int i = 0; i < origin.length; i++) {
			origin[i] = app+origin[i];
		}
		
		return origin;
	}
	
	public static String defaultString(String item, String def) {
		return item == null ? def : item;
	}
	
	public static String[] defaultStringArr(String[] item, String[] def) {
		return item == null || item.length == 0 ? def : item;
	}
	
	public static String[] arrFromString(String in, String del) {
		return in == null ? null : in.split(del);
	}
	
	public static String lineDelimiter() {
		return lineDelimiter('=', null);
	}
	
	public static String lineDelimiter(char c) {
		return lineDelimiter(c, null);
	}
	
	public static String lineDelimiter(char c, String s) {
		int len = 60;
		StringBuilder sbr = new StringBuilder();
		
		if(s != null && s.length() > 0) {
			len -= s.length()+2;
			for(int i = 0; i < len/2; ++i) {
				sbr.append(c);
			}
			sbr.append(" ");
			sbr.append(s);
			sbr.append(" ");
			for(int i = 0; i < len/2; ++i) {
				sbr.append(c);
			}
		} else {
			for(int i = 0; i < len; ++i) {
				sbr.append(c);
			}
		}
		
		return sbr.toString();
	}
	
	public static String buildString(String... vars) {
		StringBuilder bdr = new StringBuilder();
		
		for(String var: vars) {
			bdr.append(var);
		}
		
		return bdr.toString();
	}
	
	public static boolean stringIs(String[] args, int index, String search) {
		return args.length > index && args[index].equalsIgnoreCase(search);
	}
}
