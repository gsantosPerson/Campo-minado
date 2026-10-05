package br.com.gustavo.campominado;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity {
    private Game game = new Game();
    private Ranking ranking = new Ranking();
    private boolean scoreRecorded;
    private long elapsed, runningSince;
    private boolean running;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timer, status, progress, subtitle;
    private String difficulty = "Fácil";
    private HorizontalScrollView boardScroll;
    private Button clicks;
    private Board board;
    private final int navy = Color.rgb(14, 23, 40);
    private final int mint = Color.rgb(104, 232, 184);
    private final Runnable ticker = new Runnable() {
        public void run() {
            update();
            if (running) handler.postDelayed(this, 200);
        }
    };

    public void onCreate(Bundle state) {
        super.onCreate(state);
        restore();
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(navy);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(18) + insets.getSystemWindowInsetLeft(), dp(18) + insets.getSystemWindowInsetTop(),
                    dp(18) + insets.getSystemWindowInsetRight(), dp(18) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
        root.requestApplyInsets();
        root.addView(label("CAMPO MINADO", 13, mint));
        root.addView(label("Cada toque conta.", 29, Color.WHITE));
        subtitle = label("10 × 10 casas  •  15 minas", 15, Color.LTGRAY);
        subtitle.setPadding(0, dp(6), 0, dp(20));
        root.addView(subtitle);
        timer = label("00:00", 40, Color.WHITE);
        timer.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        root.addView(label("TEMPO DE JOGO", 11, mint));
        root.addView(timer);
        status = label("Toque em uma casa para começar.", 15, Color.WHITE);
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);
        board = new Board();
        boardScroll = new HorizontalScrollView(this);
        boardScroll.addView(board, new HorizontalScrollView.LayoutParams(-2, -2));
        root.addView(boardScroll, new LinearLayout.LayoutParams(-1, -2));
        Button difficultyButton = button("Escolher dificuldade", false);
        difficultyButton.setOnClickListener(v -> chooseDifficulty());
        root.addView(difficultyButton, new LinearLayout.LayoutParams(-1, dp(52)));
        progress = label("0 de 85 casas seguras", 13, Color.LTGRAY);
        progress.setPadding(0, dp(10), 0, dp(16));
        root.addView(progress);
        LinearLayout actions = new LinearLayout(this);
        clicks = button("Cliques: 0", false);
        clicks.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Contador de cliques")
                .setMessage("Toques no tabuleiro nesta partida: " + game.clicks
                        + ".\n\nCada toque conta uma vez, inclusive em casas já abertas. A abertura automática de casas não conta. O contador zera em Novo jogo.")
                .setPositiveButton("Entendi", null).show());
        Button reset = button("Novo jogo", true);
        reset.setOnClickListener(v -> {
            if (game.started && !game.ended) {
                new AlertDialog.Builder(this).setTitle("Começar de novo?")
                        .setMessage("O tempo e o contador de cliques serão zerados.")
                        .setNegativeButton("Continuar", null).setPositiveButton("Novo jogo", (d, w) -> reset()).show();
            } else reset();
        });
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, dp(54), 1);
        first.setMargins(0, 0, dp(8), 0);
        actions.addView(clicks, first);
        actions.addView(reset, new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(actions);
        Button rankingButton = button("Ranking • 10 melhores tempos", false);
        rankingButton.setOnClickListener(v -> showRanking());
        LinearLayout.LayoutParams rankingLayout = new LinearLayout.LayoutParams(-1, dp(52));
        rankingLayout.setMargins(0, dp(10), 0, 0);
        root.addView(rankingButton, rankingLayout);
        TextView tip = label("A primeira jogada é segura. Os números indicam minas nas 8 casas vizinhas. Zeros abrem a região ao redor. O tempo pausa ao sair do app. Arraste para navegar nos tabuleiros maiores.", 13, Color.LTGRAY);
        tip.setPadding(0, dp(18), 0, dp(8));
        root.addView(tip);
        update();
    }

    private String rankingKey() {
        return "ranking_" + difficulty + "_" + game.linhas + "_" + game.colunas + "_" + game.quantidadeMinas;
    }

    private void loadRanking() {
        SharedPreferences p = getSharedPreferences("game", MODE_PRIVATE);
        String fallback = difficulty.equals("Fácil") && game.linhas == 10
                && game.colunas == 10 && game.quantidadeMinas == 15
                ? p.getString("ranking", "") : "";
        ranking = Ranking.decode(p.getString(rankingKey(), fallback));
    }

    private void chooseDifficulty() {
        String[] options = {"Fácil — 10 × 10, 15 minas", "Médio — 16 × 16, 46 minas",
                "Difícil — 30 × 16, 100 minas", "Personalizado"};
        new AlertDialog.Builder(this).setTitle("Escolha a dificuldade")
                .setItems(options, (d, choice) -> {
                    if (choice == 0) confirmGame(10, 10, 15, "Fácil");
                    else if (choice == 1) confirmGame(16, 16, 46, "Médio");
                    else if (choice == 2) confirmGame(30, 16, 100, "Difícil");
                    else customGame();
                }).show();
    }

    private void confirmGame(int rows, int columns, int mines, String level) {
        if (game.started && !game.ended) {
            new AlertDialog.Builder(this).setTitle("Trocar de dificuldade?")
                    .setMessage("A partida atual será encerrada. O ranking será preservado.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Começar", (d, w) -> startGame(rows, columns, mines, level)).show();
        } else startGame(rows, columns, mines, level);
    }

    private void startGame(int rows, int columns, int mines, String level) {
        stopClock();
        save();
        game = new Game(rows, columns, mines);
        difficulty = level;
        elapsed = 0;
        scoreRecorded = false;
        loadRanking();
        board.requestLayout();
        boardScroll.scrollTo(0, 0);
        update();
        save();
    }

    private void customGame() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(20), dp(12), dp(20), dp(12));
        EditText rows = new EditText(this);
        EditText columns = new EditText(this);
        EditText mines = new EditText(this);
        rows.setHint("Linhas (2 a 40)");
        columns.setHint("Colunas (2 a 40)");
        mines.setHint("Quantidade de bombas");
        rows.setInputType(InputType.TYPE_CLASS_NUMBER);
        columns.setInputType(InputType.TYPE_CLASS_NUMBER);
        mines.setInputType(InputType.TYPE_CLASS_NUMBER);
        fields.addView(rows);
        fields.addView(columns);
        fields.addView(mines);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Personalizado")
                .setView(fields).setNegativeButton("Cancelar", null)
                .setPositiveButton("Começar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                int r = Integer.parseInt(rows.getText().toString().trim());
                int c = Integer.parseInt(columns.getText().toString().trim());
                int m = Integer.parseInt(mines.getText().toString().trim());
                if (r < 2 || r > 40) { rows.setError("Use de 2 a 40 linhas."); return; }
                if (c < 2 || c > 40) { columns.setError("Use de 2 a 40 colunas."); return; }
                if (m < 1 || m >= r * c) { mines.setError("Use de 1 a " + (r * c - 1) + " bombas."); return; }
                dialog.dismiss();
                confirmGame(r, c, m, "Personalizado");
            } catch (NumberFormatException ex) {
                mines.setError("Preencha os três campos com números inteiros.");
            }
        }));
        dialog.show();
    }

    private void showRanking() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(12));
        if (ranking.entries().isEmpty()) {
            content.addView(label("Ainda não há resultados. Vença uma partida para registrar seu tempo!", 16, Color.WHITE));
        } else {
            content.addView(label("Vitórias mais rápidas neste celular • " + ranking.entries().size() + "/10", 13, mint));
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            int position = 1;
            for (Ranking.Entry entry : ranking.entries()) {
                long seconds = entry.millis / 1000;
                String duration = String.format(Locale.ROOT, "%02d:%02d.%03d", seconds / 60, seconds % 60, entry.millis % 1000);
                TextView row = label(position + "º   " + duration + "\n" + format.format(new Date(entry.date)), 17, Color.WHITE);
                row.setPadding(0, dp(12), 0, dp(12));
                content.addView(row);
                position++;
            }
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        new AlertDialog.Builder(this).setTitle("Ranking — " + difficulty)
                .setView(scroll).setPositiveButton("Fechar", null).show();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }
    private Button button(String title, boolean primary) {
        Button button = new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextColor(primary ? navy : Color.WHITE);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(primary ? mint : Color.rgb(39, 55, 78));
        shape.setCornerRadius(dp(12));
        button.setBackground(shape);
        return button;
    }
    private long time() { return elapsed + (running ? SystemClock.elapsedRealtime() - runningSince : 0); }
    private void startClock() {
        if (!running && game.started && !game.ended) {
            running = true;
            runningSince = SystemClock.elapsedRealtime();
            handler.post(ticker);
        }
    }
    private void stopClock() {
        elapsed = time();
        running = false;
        handler.removeCallbacks(ticker);
    }
    private void reset() {
        startGame(game.linhas, game.colunas, game.quantidadeMinas, difficulty);
    }
    private void update() {
        long seconds = time() / 1000;
        timer.setText(String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60));
        clicks.setText("Cliques: " + game.clicks);
        subtitle.setText(difficulty + " • " + game.linhas + " × " + game.colunas
                + " casas • " + game.quantidadeMinas + " minas");
        progress.setText(game.revealed + " de " + (game.cells.length - game.quantidadeMinas) + " casas seguras");
        board.setContentDescription("Tabuleiro com " + game.linhas + " linhas e " + game.colunas + " colunas");
        status.setText(game.ended ? (game.won ? "Você venceu! Todas as casas seguras abertas." : "Você encontrou uma mina. Tente novamente!")
                : game.started ? "Encontre as casas seguras." : "Toque em uma casa para começar.");
        board.invalidate();
    }
    protected void onResume() { super.onResume(); startClock(); }
    protected void onPause() { stopClock(); save(); super.onPause(); }
    protected void onDestroy() { handler.removeCallbacks(ticker); super.onDestroy(); }
    private void save() {
        StringBuilder cells = new StringBuilder(), opened = new StringBuilder();
        for (int i = 0; i < game.cells.length; i++) {
            cells.append(game.cells[i]).append(',');
            opened.append(game.open[i] ? '1' : '0');
        }
        getSharedPreferences("game", MODE_PRIVATE).edit().putString("cells", cells.toString())
                .putString("open", opened.toString()).putInt("clicks", game.clicks)
                .putInt("revealed", game.revealed).putBoolean("started", game.started)
                .putBoolean("ended", game.ended).putBoolean("won", game.won).putLong("elapsed", time())
                .putString(rankingKey(), ranking.encode())
                .putInt("linhas", game.linhas).putInt("colunas", game.colunas)
                .putInt("minas", game.quantidadeMinas).putString("difficulty", difficulty).putBoolean("scoreRecorded", scoreRecorded).apply();
    }
    private void restore() {
        SharedPreferences p = getSharedPreferences("game", MODE_PRIVATE);
        try {
            game = new Game(p.getInt("linhas", 10), p.getInt("colunas", 10), p.getInt("minas", 15));
            difficulty = p.getString("difficulty", "Fácil");
        } catch (IllegalArgumentException ex) {
            game = new Game();
            difficulty = "Fácil";
        }
        loadRanking();
        scoreRecorded = p.getBoolean("scoreRecorded", p.getBoolean("ended", false));
        String[] cells = p.getString("cells", "").split(",");
        String opened = p.getString("open", "");
        if (cells.length != game.cells.length || opened.length() != game.cells.length) return;
        try {
            for (int i = 0; i < game.cells.length; i++) {
                game.cells[i] = Integer.parseInt(cells[i]);
                game.open[i] = opened.charAt(i) == '1';
            }
            game.clicks = p.getInt("clicks", 0);
            game.revealed = p.getInt("revealed", 0);
            game.started = p.getBoolean("started", false);
            game.ended = p.getBoolean("ended", false);
            game.won = p.getBoolean("won", false);
            elapsed = p.getLong("elapsed", 0);
        } catch (RuntimeException ex) { game = new Game(game.linhas, game.colunas, game.quantidadeMinas); elapsed = 0; scoreRecorded = false; }
    }

    private class Board extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float downX, downY;
        private int selected = -1;
        Board() { super(MainActivity.this); setClickable(true); setContentDescription("Tabuleiro de Campo Minado, 10 linhas e 10 colunas"); }
        protected void onMeasure(int widthSpec, int heightSpec) {
            int available = getResources().getDisplayMetrics().widthPixels - dp(36);
            int cell = Math.max(dp(32), available / game.colunas);
            setMeasuredDimension(cell * game.colunas, cell * game.linhas);
        }
        protected void onDraw(Canvas canvas) {
            float cell = getWidth() / (float) game.colunas;
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            paint.setTextSize(cell * .43f);
            paint.setTextAlign(Paint.Align.CENTER);
            int[] colors = {mint, Color.rgb(102, 180, 255), mint, Color.rgb(255, 162, 129), Color.rgb(198, 158, 255), Color.YELLOW, Color.CYAN, Color.WHITE, Color.LTGRAY};
            for (int i = 0; i < game.cells.length; i++) {
                float x = (i % game.colunas) * cell, y = (i / game.colunas) * cell;
                boolean mine = game.cells[i] == -1 && (game.ended || game.open[i]);
                paint.setColor(mine ? Color.rgb(163, 61, 72) : game.open[i]
                        ? (game.cells[i] == 0 ? Color.rgb(28, 69, 64) : Color.rgb(25, 38, 57)) : Color.rgb(47, 65, 88));
                canvas.drawRoundRect(x + 2, y + 2, x + cell - 2, y + cell - 2, dp(5), dp(5), paint);
                if (mine) {
                    paint.setColor(Color.WHITE);
                    canvas.drawCircle(x + cell / 2, y + cell / 2, cell * .14f, paint);
                    paint.setStrokeWidth(dp(2));
                    canvas.drawLine(x + cell * .25f, y + cell / 2, x + cell * .75f, y + cell / 2, paint);
                    canvas.drawLine(x + cell / 2, y + cell * .25f, x + cell / 2, y + cell * .75f, paint);
                } else if (game.open[i]) {
                    paint.setColor(colors[game.cells[i]]);
                    canvas.drawText(String.valueOf(game.cells[i]), x + cell / 2, y + cell / 2 - (paint.ascent() + paint.descent()) / 2, paint);
                }
            }
        }
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { downX = event.getX(); downY = event.getY(); return true; }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (Math.abs(event.getX() - downX) < dp(12) && Math.abs(event.getY() - downY) < dp(12)
                        && event.getX() >= 0 && event.getX() < getWidth() && event.getY() >= 0 && event.getY() < getHeight()) {
                    selected = (int) (event.getY() / (getWidth() / (float) game.colunas)) * game.colunas + (int) (event.getX() / (getWidth() / (float) game.colunas));
                    performClick();
                }
                return true;
            }
            return true;
        }
        public boolean performClick() {
            super.performClick();
            if (selected < 0 || game.ended) return true;
            game.tap(selected);
            selected = -1;
            if (game.ended) {
                stopClock();
                if (game.won && !scoreRecorded) {
                    ranking.add(elapsed, System.currentTimeMillis());
                    scoreRecorded = true;
                }
            } else startClock();
            update();
            save();
            return true;
        }
    }
}