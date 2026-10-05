package br.com.gustavo.campominado;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;

public class Game {
    public final int linhas;
    public final int colunas;
    public final int quantidadeMinas;
    public final int[] cells;
    public final boolean[] open;

    public int clicks, revealed;
    public boolean started, ended, won;

    public Game() {
        this(10, 10, 15);
    }

    public Game(int linhas, int colunas, int quantidadeMinas) {
        if (linhas < 2 || linhas > 40
                || colunas < 2 || colunas > 40) {
            throw new IllegalArgumentException(
                    "Linhas e colunas devem estar entre 2 e 40."
            );
        }

        int total = linhas * colunas;

        if (quantidadeMinas < 0 || quantidadeMinas >= total) {
            throw new IllegalArgumentException(
                    "A quantidade de minas deve estar entre 0 e " + (total - 1)
            );
        }

        this.linhas = linhas;
        this.colunas = colunas;
        this.quantidadeMinas = quantidadeMinas;
        this.cells = new int[total];
        this.open = new boolean[total];
    }

    public void generate(int safe) {
        if (safe < 0 || safe >= cells.length) {
            throw new IllegalArgumentException("Posição inválida.");
        }

        Arrays.fill(cells, 0);

        Random random = new Random();
        int mines = 0;

        while (mines < quantidadeMinas) {
            int p = random.nextInt(cells.length);

            if (p != safe && cells[p] != -1) {
                cells[p] = -1;
                mines++;
            }
        }

        for (int p = 0; p < cells.length; p++) {
            if (cells[p] == -1) {
                continue;
            }

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int r = p / colunas + dr;
                    int c = p % colunas + dc;

                    if (r >= 0 && r < linhas
                            && c >= 0 && c < colunas
                            && cells[r * colunas + c] == -1) {
                        cells[p]++;
                    }
                }
            }
        }
    }

    public void tap(int p) {
        if (p < 0 || p >= cells.length || ended) {
            return;
        }

        clicks++;

        if (!started) {
            generate(p);
            started = true;
        }

        if (open[p]) {
            return;
        }

        if (cells[p] == -1) {
            open[p] = true;
            ended = true;
            return;
        }

        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(p);

        while (!queue.isEmpty()) {
            int i = queue.removeFirst();

            if (open[i] || cells[i] == -1) {
                continue;
            }

            open[i] = true;
            revealed++;

            if (cells[i] != 0) {
                continue;
            }

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int r = i / colunas + dr;
                    int c = i % colunas + dc;

                    if (r >= 0 && r < linhas
                            && c >= 0 && c < colunas) {
                        int vizinho = r * colunas + c;

                        if (!open[vizinho] && cells[vizinho] != -1) {
                            queue.add(vizinho);
                        }
                    }
                }
            }
        }

        if (revealed == cells.length - quantidadeMinas) {
            won = true;
            ended = true;
        }
    }
}