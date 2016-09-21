package marcogino.bluemoto;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// Riferimento al db per creazione db, ecc
public class DbHelper extends SQLiteOpenHelper
{

    public DbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    /*
     * CREATE TABLE TODOS (_id INTEGER PRIMARY KEY AUTOINCREMENT, _todo TEXT, _descr TEXT)
     * */

    @Override
    public void onCreate(SQLiteDatabase db) {
        //viene esguito solo se il database è da creare (la prima volta).
        /*
        DB_NAME="TODOS_DB";
        TABLE_NAME="TODOS";
        FIELD_ID_NAME="_id";
        FIELD_TODO_NAME="_todo";
        FIELD_DESCR_NAME="_descr";
 */
        String createCom="CREATE TABLE "+DatabaseStrings.TABLE_NAME+" ("+
                DatabaseStrings.FIELD_ID_NAME+" INTEGER PRIMARY KEY AUTOINCREMENT," +
                DatabaseStrings.FIELD_SPEED_NAME+" INTEGER,"+
                DatabaseStrings.FIELD_TIME_NAME+" TEXT)";
        db.execSQL(createCom); // esecuzione query di creazione db
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // il sw passa qui se per esempio il db esiste, ma per esempio ha una versione differente (minore!!)
    }
}
