package com.oliveira.controlelocal;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.view.Gravity;
import android.widget.*;
import android.content.*;
import java.net.*;
import java.net.URLEncoder;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    LinearLayout lista, raiz;
    TextView status;
    int verde = Color.rgb(85,107,47);
    int escuro = Color.rgb(48,61,29);
    ArrayList<Dispositivo> salvos = new ArrayList<>();
    ArrayList<Dispositivo> encontrados = new ArrayList<>();
    Handler handler = new Handler(Looper.getMainLooper());
    boolean buscando = false;

    static class Dispositivo {
        String nome, ip, mac, portas;
        int cor;
        Dispositivo(String n, String i, String m, String p, int c) {
            nome=n; ip=i; mac=m; portas=p; cor=c;
        }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(escuro);
        getWindow().setNavigationBarColor(escuro);
        carregar();
        telaPrincipal();
    }

    TextView texto(String s, int tamanho) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(tamanho);
        t.setTextColor(escuro);
        return t;
    }

    Button botao(String s, Runnable acao) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(v -> acao.run());
        return b;
    }

    void preparar(String titulo) {
        raiz = new LinearLayout(this);
        raiz.setOrientation(1);
        raiz.setPadding(18,18,18,12);
        raiz.setBackgroundColor(Color.rgb(247,247,240));

        LinearLayout topo = new LinearLayout(this);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = texto(titulo,25);
        t.setTypeface(null,Typeface.BOLD);
        topo.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        topo.addView(botao("⚙", () -> menu()),new LinearLayout.LayoutParams(-2,-2));
        raiz.addView(topo);

        status = texto("",14);
        status.setPadding(0,8,0,8);
        raiz.addView(status);

        ScrollView sc = new ScrollView(this);
        lista = new LinearLayout(this);
        lista.setOrientation(1);
        sc.addView(lista);
        raiz.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(raiz);
    }

    void telaPrincipal() {
        preparar("Controle Local");
        status.setText("Dispositivos cadastrados • Rede local");
        if (salvos.isEmpty()) {
            lista.addView(texto("Nenhum dispositivo cadastrado.\n\nAbra ⚙ Configurações e procure aparelhos na rede.",17));
        }
        for (Dispositivo d : salvos) {
            Button b = botao(d.nome + "\n" + d.ip + "\n" +
                (d.mac.isEmpty() ? "MAC indisponível" : d.mac), () -> abrirControle(d));
            b.setTextColor(Color.WHITE);
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(d.cor));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,5,0,8);
            lista.addView(b,lp);
        }
    }


    android.content.SharedPreferences prefsTasmota(Dispositivo d) {
        String chave = d.ip.replaceAll("[^0-9A-Za-z]", "_");
        return getSharedPreferences("tasmota_" + chave, 0);
    }

    EditText campo(String dica, String valor, boolean senha) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(dica);
        e.setText(valor);
        if (senha) e.setInputType(129);
        return e;
    }

    void abrirControle(Dispositivo d) {
        android.content.SharedPreferences p = prefsTasmota(d);
        String usuario=p.getString("usuario", "admin");
        String senha=p.getString("senha", "");
        String porta=p.getString("porta", "80");
        String ligar=p.getString("ligar", "Power ON");
        String desligar=p.getString("desligar", "Power OFF");
        String alternar=p.getString("alternar", "Power TOGGLE");
        String teste=p.getString("teste", "Status 0");
        LinearLayout box=new LinearLayout(this); box.setOrientation(1); box.setPadding(24,8,24,0);
        TextView info=texto("IP: "+d.ip+"\nConfigure os comandos para este dispositivo Tasmota.",14); box.addView(info);
        ScrollView sc=new ScrollView(this); sc.addView(box);
        String[] acoes={"Ligar", "Desligar", "Alternar", "Testar conexão", "Executar comando personalizado", "Configurar Tasmota", "Editar nome"};
        new AlertDialog.Builder(this).setTitle(d.nome + " • " + d.ip)
            .setItems(acoes,(dlg,qual)->{
                if(qual==0) enviarTasmota(d,"ligar");
                else if(qual==1) enviarTasmota(d,"desligar");
                else if(qual==2) enviarTasmota(d,"alternar");
                else if(qual==3) enviarTasmota(d,"teste");
                else if(qual==4) enviarTasmota(d,"personalizado");
                else if(qual==5) configurarTasmota(d);
                else editar(d);
            }).show();
    }

    void configurarTasmota(Dispositivo d) {
        android.content.SharedPreferences p=prefsTasmota(d);
        LinearLayout box=new LinearLayout(this); box.setOrientation(1); box.setPadding(24,4,24,4);
        EditText ip=campo("IP ou host",p.getString("ip",d.ip),false);
        EditText porta=campo("Porta HTTP",p.getString("porta","80"),false);
        EditText usuario=campo("Usuário",p.getString("usuario","admin"),false);
        EditText senha=campo("Senha",p.getString("senha",""),true);
        EditText ligar=campo("Comando para ligar",p.getString("ligar","Power ON"),false);
        EditText desligar=campo("Comando para desligar",p.getString("desligar","Power OFF"),false);
        EditText alternar=campo("Comando para alternar",p.getString("alternar","Power TOGGLE"),false);
        EditText teste=campo("Comando de teste",p.getString("teste","Status 0"),false);
        EditText personalizado=campo("Comando personalizado / Backlog",p.getString("personalizado",""),false);
        for(EditText e:new EditText[]{ip,porta,usuario,senha,ligar,desligar,alternar,teste,personalizado}) box.addView(e);
        ScrollView sc=new ScrollView(this); sc.addView(box);
        new AlertDialog.Builder(this).setTitle("Configurar Tasmota") .setView(sc)
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Salvar",(a,b)->{
                String endereco=ip.getText().toString().trim();
                String pt=porta.getText().toString().trim();
                if(endereco.isEmpty() || pt.isEmpty()) { aviso("Informe IP e porta."); return; }
                p.edit().putString("ip",endereco).putString("porta",pt)
                 .putString("usuario",usuario.getText().toString()).putString("senha",senha.getText().toString())
                 .putString("ligar",ligar.getText().toString().trim()).putString("desligar",desligar.getText().toString().trim())
                 .putString("alternar",alternar.getText().toString().trim()).putString("teste",teste.getText().toString().trim())
                 .putString("personalizado",personalizado.getText().toString().trim()).apply();
                aviso("Configuração Tasmota salva.");
            }).show();
    }

    void aviso(String mensagem) {
        new AlertDialog.Builder(this).setMessage(mensagem).setPositiveButton("OK",null).show();
    }

    void enviarTasmota(Dispositivo d, String tipo) {
        android.content.SharedPreferences p=prefsTasmota(d);
        String ip=p.getString("ip",d.ip), porta=p.getString("porta","80");
        String usuario=p.getString("usuario","admin"), senha=p.getString("senha","");
        String comando;
        if("ligar".equals(tipo)) comando=p.getString("ligar","Power ON");
        else if("desligar".equals(tipo)) comando=p.getString("desligar","Power OFF");
        else if("alternar".equals(tipo)) comando=p.getString("alternar","Power TOGGLE");
        else if("personalizado".equals(tipo)) comando=p.getString("personalizado","");
        else comando=p.getString("teste","Status 0");
        if(comando.trim().isEmpty()) { aviso("Preencha o comando nas configurações Tasmota."); return; }
        status.setText("Enviando comando para "+ip+"...");
        new Thread(() -> {
            HttpURLConnection c=null; String resposta=""; int codigo=-1;
            try {
                String url="http://"+ip+":"+Integer.parseInt(porta)+"/cm?user="+
                    URLEncoder.encode(usuario,"UTF-8")+"&password="+URLEncoder.encode(senha,"UTF-8")+
                    "&cmnd="+URLEncoder.encode(comando,"UTF-8");
                c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(3000); c.setReadTimeout(3000); c.setRequestMethod("GET");
                codigo=c.getResponseCode(); InputStream in=(codigo>=200 && codigo<400)?c.getInputStream():c.getErrorStream();
                if(in!=null) { ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[1024]; int n; while((n=in.read(buf))!=-1) out.write(buf,0,n); resposta=new String(out.toByteArray(),"UTF-8"); in.close(); }
            } catch(Exception e) { resposta=e.getMessage()==null?"Falha na conexão":e.getMessage(); }
            finally { if(c!=null)c.disconnect(); }
            final String r=resposta; final int cod=codigo;
            handler.post(() -> aviso((cod>=200 && cod<300 ? "Resposta do Tasmota (HTTP "+cod+"):\n" : "Não foi possível confirmar o comando (HTTP "+cod+"):\n")+r));
        }).start();
    }

    void menu() {
        String[] op = {"Buscar dispositivos na rede",
            "Dispositivos cadastrados", "Sobre a descoberta"};
        new AlertDialog.Builder(this).setTitle("Configurações")
        .setItems(op,(d,n)->{
            if(n==0) buscar();
            if(n==1) gerenciar();
            if(n==2) new AlertDialog.Builder(this).setTitle("Descoberta local")
                .setMessage("A busca verifica endereços da rede e portas TCP comuns. "
                +"O MAC é mostrado quando o Android permite obtê-lo. "
                +"Encontrar um aparelho não garante que seu protocolo de controle seja compatível.")
                .setPositiveButton("OK",null).show();
        }).show();
    }

    void buscar() {
        if(buscando) return;
        WifiManager wifi = (WifiManager)getApplicationContext()
            .getSystemService(Context.WIFI_SERVICE);
        if(wifi==null || !wifi.isWifiEnabled()) {
            new AlertDialog.Builder(this).setMessage("Conecte o celular ao Wi-Fi e tente novamente.")
                .setPositiveButton("OK",null).show();
            return;
        }

        int ipNum = wifi.getDhcpInfo().ipAddress;
        if(ipNum==0) {
            new AlertDialog.Builder(this).setMessage("Não consegui ler o IP do Wi-Fi. Verifique a conexão.")
                .setPositiveButton("OK",null).show();
            return;
        }
        String meuIp = Formatter.formatIpAddress(ipNum);
        String[] partes = meuIp.split("\\.");
        if(partes.length!=4) return;
        String base=partes[0]+"."+partes[1]+"."+partes[2]+".";
        int meuUltimo=Integer.parseInt(partes[3]);

        encontrados.clear();
        preparandoBusca();
        new Thread(() -> {
            ExecutorService pool=Executors.newFixedThreadPool(40);
            ConcurrentHashMap<String,String> portasPorIp=new ConcurrentHashMap<>();
            int[] portas={80,443,8080,8888,6668,6667,7000,1883,554,5555,5683,9999};
            CountDownLatch fim=new CountDownLatch(254);

            for(int n=1;n<=254;n++) {
                final int host=n;
                if(host==meuUltimo) { fim.countDown(); continue; }
                pool.execute(() -> {
                    String ip=base+host;
                    ArrayList<Integer> abertas=new ArrayList<>();
                    for(int porta:portas) {
                        try(Socket s=new Socket()) {
                            s.connect(new InetSocketAddress(ip,porta),180);
                            abertas.add(porta);
                        } catch(Exception ignored) {}
                    }
                    if(!abertas.isEmpty()) {
                        StringBuilder sb=new StringBuilder();
                        for(int p:abertas) {
                            if(sb.length()>0) sb.append(", ");
                            sb.append(p);
                        }
                        portasPorIp.put(ip,sb.toString());
                    }
                    fim.countDown();
                });
            }

            try { fim.await(30,TimeUnit.SECONDS); } catch(Exception ignored) {}
            pool.shutdownNow();
            HashMap<String,String> macs=lerArp();

            ArrayList<Dispositivo> achados=new ArrayList<>();
            for(int n=1;n<=254;n++) {
                String ip=base+n;
                String mac=macs.getOrDefault(ip,"");
                String ps=portasPorIp.getOrDefault(ip,"");
                if(!mac.isEmpty() || !ps.isEmpty())
                    achados.add(new Dispositivo("Dispositivo "+n,ip,mac,ps,verde));
            }

            handler.post(() -> {
                buscando=false;
                encontrados=achados;
                atualizarIpsPorMac();
                salvar();
                mostrarResultados();
            });
        }).start();
    }

    void preparandoBusca() {
        buscando=true;
        preparar("Buscar dispositivos");
        status.setText("Verificando a rede Wi-Fi. Isso pode levar alguns segundos...");
        lista.addView(texto("Procurando aparelhos com serviços de rede acessíveis...",16));
    }

    HashMap<String,String> lerArp() {
        HashMap<String,String> mapa=new HashMap<>();
        try(BufferedReader br=new BufferedReader(new FileReader("/proc/net/arp"))) {
            String linha;
            br.readLine();
            while((linha=br.readLine())!=null) {
                String[] p=linha.trim().split("\\s+");
                if(p.length>=4 && p[3].matches("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")
                    && !p[3].equals("00:00:00:00:00:00"))
                    mapa.put(p[0],p[3].toUpperCase(Locale.ROOT));
            }
        } catch(Exception ignored) {}
        return mapa;
    }

    void atualizarIpsPorMac() {
        for(Dispositivo salvo:salvos) {
            if(salvo.mac.isEmpty()) continue;
            for(Dispositivo novo:encontrados) {
                if(salvo.mac.equalsIgnoreCase(novo.mac)) {
                    salvo.ip=novo.ip;
                    if(!novo.portas.isEmpty()) salvo.portas=novo.portas;
                    break;
                }
            }
        }
    }

    void mostrarResultados() {
        preparar("Resultado da busca");
        status.setText("Encontrados: "+encontrados.size()+
            ". Toque em um aparelho para ver detalhes.");
        lista.addView(botao("↻ Buscar novamente",()->buscar()));
        if(encontrados.isEmpty()) {
            lista.addView(texto("Nenhum aparelho foi identificado. Alguns dispositivos não respondem às portas verificadas, mesmo estando ligados.",16));
            return;
        }
        for(Dispositivo d:encontrados) {
            String detalhe=d.ip+
                (d.mac.isEmpty()?"\nMAC não disponível":"\nMAC: "+d.mac)+
                (d.portas.isEmpty()?"\nNenhuma porta TCP conhecida respondeu":"\nPortas abertas: "+d.portas);
            Button b=botao(detalhe,()->detalhes(d));
            b.setAllCaps(false);
            lista.addView(b);
        }
    }

    void detalhes(Dispositivo d) {
        AlertDialog.Builder dlg=new AlertDialog.Builder(this)
            .setTitle(d.ip)
            .setMessage("MAC: "+(d.mac.isEmpty()?"indisponível":d.mac)+
                "\nPortas TCP: "+(d.portas.isEmpty()?"nenhuma detectada":d.portas)+
                "\n\nA resposta de rede não confirma compatibilidade para acionar o aparelho.");
        if(!d.portas.isEmpty()) dlg.setPositiveButton("Cadastrar", (x,w)->{
            EditText nome=new EditText(this);
            nome.setSingleLine(true);
            nome.setText("Dispositivo");
            new AlertDialog.Builder(this).setTitle("Nome do aparelho").setView(nome)
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Salvar",(a,c)->{
                    String n=nome.getText().toString().trim();
                    if(n.isEmpty()) n="Dispositivo";
                    boolean existe=false;
                    if(!d.mac.isEmpty()) for(Dispositivo s:salvos)
                        if(s.mac.equalsIgnoreCase(d.mac)) {
                            s.ip=d.ip; s.portas=d.portas; existe=true; break;
                        }
                    if(!existe) {
                        d.nome=n; d.cor=verde; salvos.add(d);
                    }
                    salvar(); telaPrincipal();
                }).show();
        });
        dlg.setNegativeButton("Voltar",null).show();
    }

    void carregar() {
        String dados=getSharedPreferences("controle",0).getString("dispositivos","");
        if(dados.isEmpty()) return;
        for(String linha:dados.split("\n")) {
            String[] p=linha.split("\\|",-1);
            if(p.length==5) try {
                salvos.add(new Dispositivo(
                    new String(Base64.getDecoder().decode(p[0]),"UTF-8"),
                    p[1],p[2],p[3],Integer.parseInt(p[4])));
            } catch(Exception ignored) {}
        }
    }

    void salvar() {
        StringBuilder sb=new StringBuilder();
        for(Dispositivo d:salvos) {
            if(sb.length()>0) sb.append("\n");
            try {
                sb.append(Base64.getEncoder().encodeToString(d.nome.getBytes("UTF-8")))
                    .append("|").append(d.ip).append("|").append(d.mac)
                    .append("|").append(d.portas).append("|").append(d.cor);
            } catch(Exception ignored) {}
        }
        getSharedPreferences("controle",0).edit()
            .putString("dispositivos",sb.toString()).apply();
    }

    void editar(Dispositivo d) {
        EditText nome=new EditText(this);
        nome.setText(d.nome);
        new AlertDialog.Builder(this).setTitle("Nome do dispositivo").setView(nome)
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Salvar",(a,b)->{
                d.nome=nome.getText().toString().trim();
                if(d.nome.isEmpty()) d.nome="Dispositivo";
                salvar(); telaPrincipal();
            })
            .setNeutralButton("Excluir",(a,b)->{
                salvos.remove(d); salvar(); telaPrincipal();
            }).show();
    }

    void gerenciar() {
        if(salvos.isEmpty()) {
            new AlertDialog.Builder(this).setMessage("Ainda não há dispositivos cadastrados.")
                .setPositiveButton("OK",null).show();
            return;
        }
        String[] nomes=new String[salvos.size()];
        for(int i=0;i<salvos.size();i++) nomes[i]=salvos.get(i).nome;
        new AlertDialog.Builder(this).setTitle("Dispositivos cadastrados")
            .setItems(nomes,(d,n)->editar(salvos.get(n))).show();
    }

    @Override public void onBackPressed() {
        telaPrincipal();
    }
}
