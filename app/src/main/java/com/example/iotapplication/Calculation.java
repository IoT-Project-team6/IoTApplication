package com.example.iotapplication;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*

데스크1-1

데스크1-2 | 데스크2-1 | 데스크3-1

데스크1-3 | 데스크2-2 | 데스크3-2
----------------------------
데스크4-1 | 데스크5-1 | 데스크6-1

데스크4-2 | 데스크5-2 | 데스크6-2

데스크4-3 | 데스크5-3 | 데스크6-3
----------------------------
데스크7-1 | 데스크8-1 | 데스크9-1

        | 데스크8-2 | 데스크9-2

        | 데스크8-3 | 데스크9-3

 */

public class Calculation {

    private String[][] classMap = new String[][] {
            {"1-1-6.39", null, null},
            {"1-2-7.74", "2-1-5.39", "3-1-11.31"},
            {"1-3-3.17", "2-2-4.80", "3-2-6.53"},
            {"4-1-10.39", "5-1-2.86", "6-1-3.63"},
            {"4-2-4.61", "5-2-0.37", "6-2-6.30"},
            {"4-3-3.88", "5-3-0.89", "6-3-4.02"},
            {"7-1-3.97", "8-1-3.52", "9-1-7.84"},
            {null, "8-2-4.95", "9-2-8.47"},
            {null, "8-3-7.88", "9-3-3.40"},
    };

    public String calculateLocation(final double distance, final int transverseStep, final int lengthStep, final double direction) {
        int resultZone = -1;
        int resultTable = -1;
        double diff = 99999999;
        int left = 0;
        int right = 1;
        int line1 = 0;
        int line2 = 0;
        int line3 = 0;

        if (transverseStep > 7) {
            left = 1;
            right = 2;
        }

        if (lengthStep > 0 && lengthStep <= 2) {
            line1 = 0;
            line2 = 1;
            line3 = 1;
        } else if (lengthStep > 2 && lengthStep <= 4) {
            line1 = 0;
            line2 = 1;
            line3 = 2;
        } else if (lengthStep > 4 && lengthStep <= 6) {
            line1 = 1;
            line2 = 2;
            line3 = 3;
        } else if (lengthStep > 6 && lengthStep <= 8) {
            line1 = 2;
            line2 = 3;
            line3 = 4;
        } else if (lengthStep > 8 && lengthStep <= 10) {
            line1 = 3;
            line2 = 4;
            line3 = 5;
        } else if (lengthStep > 10 && lengthStep <= 12) {
            line1 = 4;
            line2 = 5;
            line3 = 6;
        } else if (lengthStep > 12 && lengthStep <= 14) {
            line1 = 5;
            line2 = 6;
            line3 = 7;
        } else if (lengthStep > 14 && lengthStep <= 16) {
            line1 = 6;
            line2 = 7;
            line3 = 8;
        } else if (lengthStep > 16){
            line1 = 7;
            line2 = 8;
            line3 = 8;
        }


        // 320 이상, 55 이하 일때 이동은 세로 이동으로 판단
        // 225 이상, 320 이하 오른쪽 이동 판단
        // 55 이상 145 이하 왼쪽 이동 판단
        if (direction >= 55 && direction <= 145) {
            right = left;
        } else if (direction >= 225 && direction <= 320) {
            left = right;
        }

        Log.d("hi", "lastDirection = " + direction + "");

        int[] widthCandidates = new int[] {left, right};
        int[] lengthCandidates = new int[] {line1, line2, line3};

        for (int i = 0; i < lengthCandidates.length; i++) {
            int length = lengthCandidates[i];
            for (int j = 0; j < widthCandidates.length; j++) {
                int width = widthCandidates[j];
                if (classMap[length][width] == null) {
                    continue;
                }

                String[] tableInfo = classMap[length][width].split("-");
                int zoneNumber = Integer.parseInt(tableInfo[0]);
                int tableNumber = Integer.parseInt(tableInfo[1]);
                double tableDistance = Double.parseDouble(tableInfo[2]);

                if (Math.abs(tableDistance - distance) < diff) {
                    resultZone = zoneNumber;
                    resultTable = tableNumber;
                }
            }
        }

        return resultZone + "-" + resultTable;
    }
}
