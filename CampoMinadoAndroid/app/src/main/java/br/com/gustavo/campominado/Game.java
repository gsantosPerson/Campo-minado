package br.com.gustavo.campominado;

import java.util.ArrayDeque;
import java.util.Random;

public class Game {
    public final int[] cells = new int[100];
    public final boolean[] open = new boolean[100];
    public int clicks, revealed;
    public boolean started, ended, won;

    public void generate(int safe) {
        java.util.Arrays.fill(cells, 0);
        Random random = new Random();
        int mines = 0;
        while (mines < 15) {
            int p = random.nextInt(100);
            if (p != safe && cells[p] != -1) {
                cells[p] = -1;
                mines++;
            }
        }
        for (int p = 0; p < 100; p++) {
            if (cells[p] == -1) continue;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int r = p / 10 + dr, c = p % 10 + dc;
                    if (r >= 0 && r < 10 && c >= 0 && c < 10 && cells[r * 10 + c] == -1) cells[p]++;
                }
            }
        }
    }

    public void tap(int p) {
        if (p < 0 || p >= 100 || ended) return;
        clicks++;
        if (!started) {
            generate(p);
            started = true;
        }
        if (open[p]) return;
        if (cells[p] == -1) {
            open[p] = true;
            ended = true;
            return;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(p);
        while (!queue.isEmpty()) {
            int i = queue.removeFirst();
            if (open[i] || cells[i] == -1) continue;
            open[i] = true;
            revealed++;
            if (cells[i] != 0) continue;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int r = i / 10 + dr, c = i % 10 + dc;
                    if (r >= 0 && r < 10 && c >= 0 && c < 10 && !open[r * 10 + c]) queue.add(r * 10 + c);
                }
            }
        }
        if (revealed == 85) {
            won = true;
            ended = true;
        }
    }
}
