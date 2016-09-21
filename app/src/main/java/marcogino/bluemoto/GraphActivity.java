package marcogino.bluemoto;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * Created by Marco_76 on 29/08/2016.
 */
public class GraphActivity extends AppCompatActivity {

    private RelativeLayout mainLayout;
    private LineChart lineChart;
    private static int NumberOfPointYouCareAbout = 10;
    boolean mIsBound;
    Messenger mService = null;
    private int speed=0;
    private Button btDeleteDBdata;
    private Button btPostData;
    private Button btPrevData;
    private Button btPostDataM;
    private Button btPrevDataM;
    private CursorAdapter adapter=null;
    Cursor cursor;
    Context context;
    private DbManager dbManager;
    private LineData data;
    LineDataSet set;
    ArrayList<String> labels;
    private int idRecordMax = 0;
    private int idRecordMin = 0;
    private int idRecordMaxDB = 0;
    private int idRecordMinDB = 0;
    private int idMin = 0;
    private int idMax = 0;
    private Button btViewBD;




    /**
     * Reference to a Handler, which others can use to send messages to it.
     * This allows for the implementation of message-based communication across
     * processes, by creating a Messenger pointing to a Handler in one process,
     * and handing that Messenger to another process.
     * */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String str1="";
            switch (msg.what) {

                case MotoConnectionService.MSG_SET_STRING_VALUE:
                    try
                    {
                        Bundle b = (Bundle)msg.getData();
                        str1 = b.get("str1").toString();

                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    if(str1.startsWith("SP"))
                    {
                        Double velocita = Double.parseDouble(str1.substring(2));
                        speed = (int) velocita.floatValue();
                        if(Constants.LOGGING_ENABLED == true) //se è abilitato il logging, prelevo le info dal db altrimenti visualizzo quelle in tempo reale
                        {
                            getLastSpeedFromDB();

                        }
                        else
                        {
                            addEntry(speed, getDateTime());
                        }


                    }



                default:
                    super.handleMessage(msg);
            }
        }
    }

    private String getDateTime ()
    {
        DateFormat df = new SimpleDateFormat("dd:MMM HH:mm:ss");
        String localTime = df.format(Calendar.getInstance().getTime());
        return localTime;

    }

    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (MotoConnectionService.isRunning()) { //Utilizzo il metodo statico per testarne lo stato.
            doBindService();

        }
    }
    void doBindService() {
        bindService(new Intent(this, MotoConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

        //MotoConnectionService.rxOn = false;
    }
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service); // restituisce istanza del servizio connesso

            try {
                Message msg = Message.obtain(null, MotoConnectionService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger; // mMessenger:  Riferimento a un gestore , che altri possono utilizzare per inviare messaggi ad esso .
                mService.send(msg); //mService: istanza del servizio ottenuta dal binding
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it

            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;


        }
    };




    private View.OnClickListener btDeleteDBdataClickListener =new View.OnClickListener()
    {
        @Override
        public void onClick(final View v) {
            AlertDialog.Builder builder=new AlertDialog.Builder(GraphActivity.this);
            builder.setTitle("Attenzione: operazione irreversibile");
            builder.setMessage("Sicuri di voler cancellare i dati?");
            builder.setPositiveButton("Sì", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    if(dbManager == null)
                    {
                        dbManager  = new DbManager(context);
                    }
                    dbManager.delete();


                    getAllSpeedFromDB();

                }
            });
            builder.setNegativeButton("No", null);
            builder.show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         context = this;


        setContentView(R.layout.activity_graf);

        btDeleteDBdata= (Button)findViewById(R.id.btDeleteDBdata); // aggiungo il listener per il click sul bottone
        btPrevData= (Button)findViewById(R.id.btPrevData);
        btPostData= (Button)findViewById(R.id.btPostData);

        btPostData.setOnClickListener(postData);
        btPrevData.setOnClickListener(prevData);


        btPrevDataM= (Button)findViewById(R.id.btPrevDataM);
        btPostDataM= (Button)findViewById(R.id.btPostDataM);

        btPostDataM.setOnClickListener(postData);
        btPrevDataM.setOnClickListener(prevData);

        CheckIfServiceIsRunning();



        btDeleteDBdata.setOnClickListener(btDeleteDBdataClickListener);
        mainLayout = (RelativeLayout)findViewById(R.id.relativeLayout);
        lineChart = (LineChart)findViewById(R.id.chart);



        //castomizzo la linea del grafico
        lineChart.setDescription("");
        lineChart.setNoDataTextDescription("");

        //metto in evidenza il valore
        lineChart.setHighlightPerDragEnabled(true);

        //abilito il touch gesture
        lineChart.setTouchEnabled(true);

        //abilito scaling e dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDragEnabled(true);

        //disabilto la griglia dello sfondo
        lineChart.setDrawGridBackground(false);

        //abilito il pinch zoom per evitare lo scaling x e y separatamente
        lineChart.setPinchZoom(true);

        //colore di sfondo altrnativo
        lineChart.setBackgroundColor(Color.LTGRAY);

        //adesso lavoriamo con i dati...
        LineData lineData = new LineData();
        lineData.setValueTextColor(Color.WHITE);

        //aggiungo i dati al grafico
        lineChart.setData(lineData);



        //ottengo la legenda dell'oggetto
        Legend l = lineChart.getLegend();

        //personalizzo la legenda
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = lineChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);

        YAxis yl  = lineChart.getAxisLeft();
        yl.setTextColor(Color.WHITE);
        yl.setAxisMaxValue(250);
        yl.setDrawGridLines(true);

        //nessun asse a destra
        YAxis yl2 = lineChart.getAxisRight();
        yl2.setEnabled(false);

        getAllSpeedFromDB();


    }



    View.OnClickListener prevData = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            if(view.getId() == R.id.btPrevData) {
                getRangeSpeedFromDB(true, false); // se passo parametro true, chiedo dati precedenti. Il secondo parametro, se a true traslo grafico di 1.
            }
            else
            {
                getRangeSpeedFromDB(true, true);
            }


        }
    };
    View.OnClickListener postData = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            if(view.getId() == R.id.btPostData) {
                getRangeSpeedFromDB(false, false); // se passo parametro false, chiedo dati successivi (se presenti). Il secondo parametro, se a true traslo grafico di 1.
            }
            else
            {
                getRangeSpeedFromDB(false, true);
            }
        }
    };
    private void getAllSpeedFromDB()
    {
        if(dbManager == null) {
            dbManager = new DbManager(this);
        }
        if(cursor==null) {
            cursor = dbManager.getSpeeds(); // ci restituisce un cursore con tutti i dati nella tabella
        }
        // viene quindi assegnato il cursore al CursorAdapter pe creare l'elenco di View
try {


        if (cursor.moveToFirst()) {
            do {
                String speed;
                String time;
                try {
                    speed = cursor.getString(cursor.getColumnIndex("_speed"));
                    time = cursor.getString(cursor.getColumnIndex("_time"));
                    idRecordMax = cursor.getInt(cursor.getColumnIndex("_id")); //ottengo l'ID dell'ultimo recor inserito nel grafico
                }
                catch (Exception e)
                {
                    break;
                }

                // do what ever you want here
                try {
                    addEntry(Integer.parseInt(speed), time);
                } catch (Exception e) {

                }


            } while (cursor.moveToNext());
        }
    }
    catch (Exception e)
        {
            set.clear();

        }
        cursor.close();
    }

    private void getRangeSpeedFromDB(Boolean datiPrecedenti, Boolean fastSearch) {

        try {
            set.clear();
        }
        catch (Exception e)
        {}

        ArrayList<Entry> entries = new ArrayList<Entry>();
        ArrayList<String> labels = new ArrayList<String>();

        if(dbManager == null) {
            dbManager = new DbManager(this);
        }
        Cursor c = dbManager.getLastDbIdRow(); // ricevo l'ID dell'ultima riga del database
        if( c != null && c.moveToFirst() ) {
            idRecordMaxDB = c.getInt(0);// numero id max nel DB
        }
        c.close();

        c = dbManager.getFirstDbIdRow(); //Id della prima riga del database
        if( c != null && c.moveToFirst() ) {
            idRecordMinDB = c.getInt(0);// numero id min nel DB
        }
        c.close();

        //definisco l'ID minimo e max di cui fare la query

        setMinMaxRecordDb(datiPrecedenti, fastSearch);



        try {
            cursor.close();
            cursor = dbManager.getRangeSpeeds(idMin,idMax, NumberOfPointYouCareAbout); // ci restituisce un cursore con tutti i dati nella tabella comprese
            //tra id min e id max passati com prametro

        }
        catch (IllegalArgumentException e)
        {
            Toast.makeText(this, "Non ci sono ulteriori dati", Toast.LENGTH_SHORT).show();
        }

        // in questo punto ho a disposizione un cursore di max "NumberOfPointYouCareAbout" record da visualizzare nel grafico
        int indice = 0;
        if (cursor.moveToFirst()){
            do{
                try {
                    String speed = cursor.getString(cursor.getColumnIndex("_speed"));
                    String time = cursor.getString(cursor.getColumnIndex("_time"));

                    entries.add(new Entry(Integer.parseInt(speed),indice));
                    labels.add(time);
                    indice++;
                }
                   catch (Exception e)
                   {
                       Toast.makeText(this, "Errore lettura DB",Toast.LENGTH_SHORT).show();
                   }

               // updateEntry(Integer.parseInt(speed), time);

                }
            while(cursor.moveToNext());

        }
        cursor.close();


        set = createSetPopoled(entries);

        set.notifyDataSetChanged();

        LineData ld = new LineData(labels,set);
        lineChart.setData(ld);

        lineChart.notifyDataSetChanged();
        lineChart.invalidate();

    }

    private Boolean setMinMaxRecordDb(Boolean datiPrecedenti, Boolean fastSearch) {

        if (fastSearch == false)// vado avanti e indietro di un singolo step per volta
        {
            if (datiPrecedenti == true)//chiedo dati precendti
            {
                if ((idRecordMax - 1 - NumberOfPointYouCareAbout) < idRecordMinDB) { // se soddisfatto significa che ho già raggiunto il primo record del DB.
                    idMax = idRecordMax = idRecordMax - 1;
                    idMin = idRecordMin = idRecordMinDB;
                } else {
                    idMax = idRecordMax = idRecordMax - 1;
                    idMin = idRecordMin = idMax - NumberOfPointYouCareAbout;
                }



            } else        // chiedo dati successivi
            {
                if ((idRecordMax + 1) > idRecordMaxDB) {
                    idMax = idRecordMax = idRecordMaxDB;

                    if (idMax - NumberOfPointYouCareAbout > idRecordMinDB) {
                        idMin = idMax - NumberOfPointYouCareAbout;
                    } else {
                        idMin = idRecordMinDB;
                    }

                } else {
                    idMax = idRecordMax = idRecordMax + 1;
                    if (idMax - NumberOfPointYouCareAbout > idRecordMinDB) {
                        idMin = idMax - NumberOfPointYouCareAbout;
                    } else {
                        idMin = idRecordMinDB;
                    }
                }


            }

        }
        else // procedo di "NumberOfPointYouCareAbout" step per volta....
        {
            if (datiPrecedenti == true)//chiedo dati precendti
            {
                if ((idRecordMax - NumberOfPointYouCareAbout - NumberOfPointYouCareAbout) < idRecordMinDB) { // se soddisfatto significa che ho già raggiunto il primo record del DB.
                    idMax = idRecordMax = idRecordMax - NumberOfPointYouCareAbout;
                    idMin = idRecordMin = idRecordMinDB;
                } else {
                    idMax = idRecordMax = idRecordMax - NumberOfPointYouCareAbout;
                    idMin = idRecordMin = idMax - NumberOfPointYouCareAbout;
                }



            } else        // chiedo dati successivi
            {
                if ((idRecordMax + NumberOfPointYouCareAbout) > idRecordMaxDB) {
                    idMax = idRecordMax = idRecordMaxDB;

                    if (idMax - NumberOfPointYouCareAbout > idRecordMinDB)
                    {
                        idMin = idMax - NumberOfPointYouCareAbout;
                    }
                    else
                    {
                        idMin = idRecordMinDB;
                    }
                }
                else
                {
                    idMax = idRecordMax = idRecordMax + NumberOfPointYouCareAbout;
                    if (idMax - NumberOfPointYouCareAbout > idRecordMinDB)
                    {
                        idMin = idMax - NumberOfPointYouCareAbout;
                    }
                    else
                    {
                        idMin = idRecordMinDB;
                    }
                }

            }

        }

        return true;
    }
    private void getLastSpeedFromDB() //ultimo dato del db da inserire nel grafico
    {
        if(dbManager == null) {
            dbManager = new DbManager(this);
        }

            cursor = dbManager.getSpeeds(); // ci restituisce un cursore con tutti i dati nella tabella


        if (cursor.moveToLast()){
            do{
                String speed = cursor.getString(cursor.getColumnIndex("_speed"));
                String time = cursor.getString(cursor.getColumnIndex("_time"));
                idRecordMax = cursor.getInt(cursor.getColumnIndex("_id")); //ottengo l'ID dell'ultimo recor inserito nel grafico
                // do what ever you want here
                if(Constants.SPEED > Constants.SPEED_MIN_TO_RECORD) {
                    addEntry(Integer.parseInt(speed), time);
                }
                else
                {
                    addEntry(Integer.parseInt("0"), time);
                }

            }while(cursor.moveToNext());
        }
        cursor.close();
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        //simulo l'imissione dati in tempo reale

        /*
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i<=100; i++)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addEntry();
                        }
                    });

                    //pausa tra un ingresso e l'altro
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
        */
    }

    //Ho bisogno di un metodo per aggiungere i dati alla linea del grafico
    private void addEntry(int speed, String time)
    {
        data = lineChart.getData();
        if(data != null)
        {
            set = (LineDataSet) data.getDataSetByIndex(0);
            if(set == null)
            {
                //se nullo lo creo
                set = createSet();
                data.addDataSet(set);
            }

            //aggiungo un valore
            data.addXValue(time);


            data.addEntry(
                    new Entry(speed, set.getEntryCount()), 0);


            if(set.getEntryCount() == NumberOfPointYouCareAbout) {
                data.removeXValue(0);
                set.removeEntry(0);

                for (Entry entry : set.getYVals()) {
                    entry.setXIndex(entry.getXIndex() - 1);
                }
            }


            //NOTIFICO AL GRAFICO CHE QUALCOSA E' CAMBIATO
            lineChart.notifyDataSetChanged();


            //lineChart.setVisibleXRangeMaximum(15);

            //lineChart.moveViewTo(data.getDataSetCount() - 7, 50f, YAxis.AxisDependency.LEFT);

            //scroll all' ultimo dato in ingresso
            lineChart.moveViewToX(data.getDataSetCount());



        }

    }
    //Ho bisogno di un metodo per aggiungere i dati alla linea del grafico
    private void updateEntry(int speed, String time)
    {

        lineChart.getData();
        if(set == null)
        {
            //se nullo lo creo
            set = createSet();
            data.addDataSet(set);
        }
        data.addXValue(time);
        data.addEntry(
                new Entry(speed, set.getEntryCount()), 0);



        data.notifyDataChanged();
        lineChart.notifyDataSetChanged();
       // lineChart.moveViewToX(data.getDataSetCount());

    }

    private LineDataSet createSet()
    {
        LineDataSet set = new LineDataSet(null, "Speed");
        set.setDrawCircles(true);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setDrawCubic(true);

        set.setLineWidth(2f);
        set.setCircleSize(2f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244,117,177));
        set.setValueTextColor(Color.BLUE);
        set.setValueTextSize(8f);


        return  set;






    }
    //metodo per creare il set
    private LineDataSet createSetPopoled(ArrayList<Entry> entries)
    {
        LineDataSet set = new LineDataSet(entries, "Speed");
        set.setDrawCircles(true);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setDrawCubic(true);

        set.setLineWidth(2f);
        set.setCircleSize(2f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244,117,177));
        set.setValueTextColor(Color.BLUE);
        set.setValueTextSize(8f);


        return  set;






    }
}
