package applications.master.com.onlineattendance;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by RASHID on 4/28/2018.
 */

public class DataHelper extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "hotspot.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_STUDENT_INFO = "StudentInfo";

    private static final String ID = "Id";
    private static final String StudentName = "StudentName";
    private static final String StudentAttendance = "StudentAttendance";



    public DataHelper(Context context)
    {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }


    public void createTables(SQLiteDatabase  db)
    {
        db.execSQL("CREATE TABLE " + TABLE_STUDENT_INFO + " ( " + ID+" INTEGER PRIMARY KEY AUTOINCREMENT , " +StudentName + " TEXT  , "
                + StudentAttendance + " TEXT );"
        );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        createTables(db);


    } // end of oncreate database here

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
