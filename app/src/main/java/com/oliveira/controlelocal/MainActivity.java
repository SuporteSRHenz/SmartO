package com.oliveira.controlelocal;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;

public class MainActivity extends Activity {
    LinearLayout lista;
    int cor = Color.rgb(85, 107, 47);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(48, 61, 29));
        getWindow().setNavigationBarColor(Color.rgb(48, 61, 29));

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setPadding(20, 24, 20, 16);
        raiz.setBackgroundColor(Color.rgb(247, 247, 240));

        TextView titulo = new TextView(this);
        titulo.setText("Controle Local");
        titulo.setTextSize(27);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(Color.rgb(48, 61, 29));
        raiz.addView(titulo);

        TextView subtitulo = new TextView(this);
        subtitulo.setText("Automação na sua rede local");
        subtitulo.setTextSize(15);
        subtitulo.setTextColor(Color.DKGRAY);
        subtitulo.setPadding(0, 4, 0, 20);
        raiz.addView(subtitulo);

        Button adicionar = new Button(this);
        adicionar.setText("+ Adicionar dispositivo");
        adicionar.setOnClickListener(v -> abrirCadastro());
        raiz.addView(adicionar);

        TextView aviso = new TextView(this);
        aviso.setText("Os dispositivos só deverão ser adicionados após o teste de comunicação.");
        aviso.setTextSize(13);
        aviso.setTextColor(Color.DKGRAY);
        aviso.setPadding(0, 12, 0, 16);
        raiz.addView(aviso);

        ScrollView scroll = new ScrollView(this);
        lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(lista);
        raiz.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView rodape = new TextView(this);
        rodape.setText("Controle local • Protótipo inicial");
        rodape.setGravity(Gravity.CENTER);
        rodape.setTextColor(Color.GRAY);
        rodape.setPadding(0, 12, 0, 0);
        raiz.addView(rodape);

        setContentView(raiz);
    }

    void abrirCadastro() {
        LinearLayout campos = new LinearLayout(this);
        campos.setOrientation(LinearLayout.VERTICAL);
        EditText nome = new EditText(this);
        nome.setHint("Nome do dispositivo");
        campos.addView(nome);

        TextView info = new TextView(this);
        info.setText("O cadastro visual não testa nem aciona o relé ainda.");
        info.setPadding(0, 8, 0, 8);
        campos.addView(info);

        new AlertDialog.Builder(this)
            .setTitle("Novo dispositivo")
            .setView(campos)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Criar botão de teste", (d, w) -> {
                String n = nome.getText().toString().trim();
                if (n.isEmpty()) n = "Dispositivo";
                criarBotao(n);
            }).show();
    }

    void criarBotao(String nome) {
        Button botao = new Button(this);
        botao.setAllCaps(false);
        botao.setText(nome + "\nNão conectado");
        botao.setTextColor(Color.WHITE);
        botao.setBackgroundTintList(android.content.res.ColorStateList.valueOf(cor));
        botao.setMinHeight(100);
        botao.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle(nome)
                .setMessage("Este botão ainda não está ligado a um relé real. A comunicação local será implementada após o teste do dispositivo.")
                .setPositiveButton("Entendi", null)
                .show()
        );
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 12);
        lista.addView(botao, lp);
    }
}
