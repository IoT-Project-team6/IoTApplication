package com.example.iotapplication;

import java.util.ArrayList;
import java.util.List;

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
            {"1-1-10", null, null},
            {"1-2-8.5", "2-1-5.5", "3-1-8.5"},
            {"1-3-7", "2-2-4", "3-2-7"},
            {"4-1-5.5", "5-1-2.5", "6-1-5.5"},
            {"4-2-4", "5-2-1.5", "6-2-4"},
            {"4-3-5.5", "5-3-2.5", "6-3-5.5"},
            {"7-1-7", "8-1-4", "9-1-7"},
            {null, "8-2-5.5", "9-2-8.5"},
            {null, "8-3-7", "9-3-10"},
    };

    public String calculateLocation(final double distance, final int transverseStep, final int lengthStep) {
        int resultZone = -1;
        int resultTable = -1;
        double diff = 99999999;
        int left = 0;
        int right = 1;
        int up = 0;
        int down = 0;

        if (transverseStep > 5) {
            left = 1;
            right = 2;
        }

        if (lengthStep > 6 && lengthStep <= 12) {
            up = 0;
            down = 1;
        } else if (lengthStep > 12 && lengthStep <= 18) {
            up = 1;
            down = 2;
        } else {
            up = 2;
            down = 2;
        }

        int[] widthCandidates = new int[] {left, right};
        int[] lengthCandidates = new int[] {up, down};

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
