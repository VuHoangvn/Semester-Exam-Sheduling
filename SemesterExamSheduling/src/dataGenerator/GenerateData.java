package dataGenerator;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class GenerateData {
	Random R = new Random();
	class Conflict{
		int i; int v;
		public Conflict(int i, int v) {
			this.i = i;
			this.v = v;
		}
	}
	
	public void genData() {
		String dataPath = "data/";
		File dir = new File(dataPath);
		File[] fileList = dir.listFiles();
		int num_subjects;
		int num_conflicts;
		int num_rooms;
		int[] students;
		Conflict[] conflicts;
		int[] room_seats;
		
		for (File file: fileList) {
			try {
				Scanner in = new Scanner(new File(file.toString()));
				num_subjects = in.nextInt();
				num_conflicts = in.nextInt();
				students = new int[num_subjects];
				conflicts = new Conflict[num_conflicts];
				num_rooms = R.nextInt((int)num_subjects/3) + (int)num_subjects/3;
				room_seats = new int[num_rooms];
				
				for (int k = 0; k < num_rooms; k++) {
					room_seats[k] = R.nextInt(180) + 20;
				}
				
				for (int k = 0; k < num_subjects; k++) {
					students[k] = R.nextInt(150) + 30;
				}
				
				for (int k = 1; k <= num_conflicts; k++) {
					int i = in.nextInt();
					int v = in.nextInt();
					conflicts[k-1] = new Conflict(i, v);
				}
				
				in.close();
				System.out.println(num_subjects + " " + num_conflicts + " " + num_rooms);
				try {
					PrintWriter out = new PrintWriter(file.toString());
					out.print(num_subjects);
					out.print(" ");
					out.print(num_conflicts);
					out.print(" ");
					out.println(num_rooms);
					for (int k = 0; k < num_subjects; k++) {
						out.print(students[k] + " ");
					}
					out.println();
					
					for (int k = 0; k < num_rooms; k++) {
						out.print(room_seats[k] + " ");
					}
					out.println();
					
					for (int k = 0; k < num_conflicts; k++) {
						out.println(conflicts[k].i + " " + conflicts[k].v);
					}
					
					out.close();
				}catch(Exception e) {
					e.printStackTrace();
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String[] args){
		GenerateData gen = new GenerateData();
		gen.genData();
	}

}
