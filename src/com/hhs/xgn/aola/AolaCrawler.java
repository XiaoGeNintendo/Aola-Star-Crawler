package com.hhs.xgn.aola;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class AolaCrawler {

	public static void main(String[] args) {
		AolaCrawler ac=new AolaCrawler();
		ac.solve();
	}


	
	public void solve(){
		Scanner sc=new Scanner(System.in);
		
		System.out.println("Please input the range of yabi:");
		int l=sc.nextInt();
		int r=sc.nextInt();
		
		System.out.println("Please input format(gif/svg):");
		String format=sc.next();
		
		System.out.println("Skip download?");
		boolean cwork=sc.nextBoolean();
		System.out.println("Skip decompress?");
		boolean cdecom=sc.nextBoolean();
		
		for(int i=l;i<=r;i++){
			Thread t=new YabiThread(i,format,cwork,cdecom);
			t.setName("Yabi"+i);
			t.start();
		}
		
		sc.close();
	}
}
