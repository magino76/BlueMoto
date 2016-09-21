package marcogino.bluemoto;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

// metodi che servono per effettuare operazioni sul db
public class DbManager
{
    private Context context;
    private DbHelper dbHelper;

    public DbManager(Context context) {
        this.context = context;
        dbHelper=new DbHelper(context,DatabaseStrings.DB_NAME, null, DatabaseStrings.DB_VERSION);
        //punto in cui viene creato il DB usando il DBHelper... (ed evntualmente un ubgrade a seconda della versione)
    }

    /*
    *  INSERT INTO nometab
    *  (nome, cognome, eta) VALUES ("luca","rossi", 18)
    * */

    public Cursor getSpeeds()
    {
        SQLiteDatabase db=dbHelper.getReadableDatabase();
        Cursor c=db.query(DatabaseStrings.TABLE_NAME, null, null, null, null, null, null);
        return c;
    }

    public Cursor getRangeSpeeds(int idMin, int idMax, int nLimit) //eseguire query con il range corretto
    {
        SQLiteDatabase db=dbHelper.getReadableDatabase();
       // String[] whereArgs=new String[]{String.valueOf(idMin), String.valueOf(idMax)};
       // Cursor c=db.query(DatabaseStrings.TABLE_NAME,null,DatabaseStrings.FIELD_ID_NAME +">=? and "+
               // DatabaseStrings.FIELD_ID_NAME +"<=?", whereArgs,null,null,null, String.valueOf(nLimit));

        String query = "SELECT * FROM SPEEDS WHERE _id >= "+ idMin + " and _id <= "+ idMax + " LIMIT "+ nLimit;
        Cursor c =db.rawQuery(query , null);
        return c;
    }

    public Cursor getLastDbIdRow()
    {

        String query = "SELECT MAX(_id) as _id FROM SPEEDS;";
        SQLiteDatabase db=dbHelper.getReadableDatabase();
        Cursor c =db.rawQuery(query , null);
        return c;
    }

    public Cursor getFirstDbIdRow()
    {

        String query = "SELECT MIN(_id) as _id FROM SPEEDS;";
        SQLiteDatabase db=dbHelper.getReadableDatabase();
        Cursor c =db.rawQuery(query , null);
        return c;
    }

    public void saveSpeed(int speed, String time)
    {
        SQLiteDatabase db=dbHelper.getWritableDatabase();// recupero riferimento al DB (lettura o scrittura)
        ContentValues contents=new ContentValues();
        contents.put(DatabaseStrings.FIELD_SPEED_NAME, speed);
        contents.put(DatabaseStrings.FIELD_TIME_NAME, time);

        db.insert(DatabaseStrings.TABLE_NAME, null, contents);
        db.close();

    }


    public void delete()
    {
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM "+ DatabaseStrings.TABLE_NAME);
        db.close();
        //int v=db.delete(DatabaseStrings.TABLE_NAME, null,null);
        //db.close();
    }

    public ArrayList<Cursor> getData(String Query){
        //get writable database
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        String[] columns = new String[] { "mesage" };
        //an array list of cursor to save two cursors one has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
        MatrixCursor Cursor2= new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);


        try{
            String maxQuery = Query ;
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(maxQuery, null);


            //add value to cursor2
            Cursor2.addRow(new Object[] { "Success" });

            alc.set(1,Cursor2);
            if (null != c && c.getCount() > 0) {


                alc.set(0,c);
                c.moveToFirst();

                return alc ;
            }
            return alc;
        } catch(SQLException sqlEx){
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+sqlEx.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        } catch(Exception ex){

            Log.d("printing exception", ex.getMessage());

            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+ex.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        }


    }
}
