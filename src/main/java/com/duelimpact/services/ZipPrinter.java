package com.duelimpact.services;

import java.util.concurrent.TimeUnit;

import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class ZipPrinter {

	private static final char[] spinSigns = { '/', '-', '\\', '|' };
	private static final int BAR_NOTCHES = 30;
	private static int spinner = 0;
	private static int loadLength;
	private static int maxFiles;
	private static String zipName;

	private static BehaviorSubject<Integer> zipSubject;

	static {
		zipSubject = BehaviorSubject.create();
		zipName = "...";
	}

	public static void subscribe() {
		zipSubject.observeOn(Schedulers.newThread()).sample(30l, TimeUnit.MILLISECONDS).subscribe(progress -> {

			int digit = digitCount(maxFiles);

			String number = String.format("%" + digit + "d / %d", Math.min(progress, maxFiles), maxFiles);
			String loadingBar = buildLoadingBar(progress, maxFiles);
			String currentLoad = String.format("%s - %s: %s", loadingBar, number, zipName);
			loadLength = Math.max(currentLoad.length(), loadLength);

			System.out.printf("%-" + loadLength + "s", currentLoad);

			if (progress >= maxFiles) {
				System.out.println("\nArchive Update Complete.");
			} else {
				System.out.print("\r");
			}
		});
	}

	public static void setMaxFiles(int max) {
		maxFiles = max;
	}

	public static void setFileName(String zip) {
		zipName = zip;
	}

	public static void updateSubject(Integer progress) {
		zipSubject.onNext(progress);
	}

	private static String buildLoadingBar(int current, int max) {

		int percentage = (current * 100) / max;
		spinner = (spinner + 1) % spinSigns.length;
		char spinSign = spinSigns[spinner];

		if (percentage == 100) {
			spinSign = '+';
		}

		String percNum = String.format("%3d%s ", percentage, "%");

		StringBuilder bar = new StringBuilder(percNum);

		for (int i = 0; i < BAR_NOTCHES; i++) {
			if (percentage - i * (100 / BAR_NOTCHES) > 0) {
				bar.append("█");
			} else {
				bar.append("░");
			}
		}
		bar.append(" " + spinSign);

		return bar.toString();
	}

	private static int digitCount(int amount) {
		int index = 0;

		while (amount != 0) {
			amount /= 10;
			index++;
		}

		return Math.max(index, 1);
	}

}
