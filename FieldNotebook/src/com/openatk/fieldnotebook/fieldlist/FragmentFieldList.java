package com.openatk.fieldnotebook.fieldlist;

import java.util.List;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openatk.fieldnotebook.FragmentDrawing;
import com.openatk.fieldnotebook.R;
import com.openatk.fieldnotebook.ScrollAutoView;
import com.openatk.fieldnotebook.FragmentDrawing.DrawingListener;
import com.openatk.fieldnotebook.db.DatabaseHelper;
import com.openatk.fieldnotebook.db.Field;
import com.openatk.fieldnotebook.db.Note;
import com.openatk.fieldnotebook.db.TableNotes;
import com.openatk.fieldnotebook.drawing.MyMarker;
import com.openatk.fieldnotebook.drawing.MyPolygon;
import com.openatk.fieldnotebook.drawing.MyPolyline;
import com.openatk.fieldnotebook.slider.SliderListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class FragmentFieldList extends Fragment {
	private static String TAG = FragmentFieldList.class.getName();
	
	private FragmentFieldList me = null;

	private ScrollAutoView scrollView;
	private LinearLayout list;
	
	private FieldListListener listener;
	private Field currentField = null;
	private List<Field> fields = null;
	
	private DatabaseHelper dbHelper;
	
	LayoutInflater vi;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_field_list, container, false);

		me = this;
		
		scrollView = (ScrollAutoView) view.findViewById(R.id.field_list_scrollView);
		list = (LinearLayout) view.findViewById(R.id.field_list_list);
		
		dbHelper = new DatabaseHelper(this.getActivity());
		vi = (LayoutInflater) this.getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		Fragment parentFragment = getParentFragment();
		if (parentFragment != null && parentFragment instanceof FieldListListener) {
			// Check if parent fragment (if there is one) is listener
			listener = (FieldListListener) parentFragment;
		} else if (activity != null && activity instanceof FieldListListener) {
			// Otherwise, check if parent activity is listener
			listener = (FieldListListener) activity;
		} else if(parentFragment != null && parentFragment instanceof FieldListParentListener){
			//Otherwise check if parent fragment knows who the listener is
			listener = ((FieldListParentListener)parentFragment).FieldListGetListener();
		} else if(activity != null && activity instanceof FieldListParentListener){
			//Otherwise check if parent activity knows who the listener is
			listener = ((FieldListParentListener)activity).FieldListGetListener();
		}
		else if (listener == null) {
			Log.w(TAG, "onAttach: neither the parent fragment or parent activity implement NoteListListener");
			throw new ClassCastException("Parent Activity or parent fragment must implement NoteListListener");
		}
		Log.d(TAG, "Attached");
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listener.FieldListRequestData(this);
	}

	public void populateData(Integer currentFieldId) {
		Log.d(TAG, "PopulateData");
				
		//Clear current
		list.removeAllViews();
		
		//Get current field
		currentField = null;
		fields = Field.FindAllFieldsNotDeleted(dbHelper.getReadableDatabase());
		dbHelper.close();
				
		for(int i=0; i<fields.size(); i++){
			if(currentFieldId != null && fields.get(i).getId() == currentFieldId){
				this.currentField = fields.get(i);
			}
			//Add to list
			list.addView(inflateField(fields.get(i)));
		}
	}
		
	private View inflateField(Field field){
		View view = vi.inflate(R.layout.field, null);
		FieldView newView = new FieldView();
		newView.layField = (RelativeLayout) view.findViewById(R.id.field);
		newView.tvName = (TextView) view.findViewById(R.id.field_txtName);
		newView.tvAcres = (TextView) view.findViewById(R.id.field_txtAcres);
		newView.row1 = (RelativeLayout) view.findViewById(R.id.field_row1);
		newView.row2 = (RelativeLayout) view.findViewById(R.id.field_row2);
		newView.field = field;
		
		newView.tvName.setText(field.getName());
		newView.tvAcres.setText(Integer.toString(field.getAcres()) + " acres");
		
		newView.layField.setTag(newView);
		newView.layField.setOnClickListener(fieldClickListener);
		
		newView.me = view;
		view.setTag(newView);
		return view;
	}
	
	private OnClickListener fieldClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			FieldView theView = (FieldView) v.getTag();
			if(v.getId() == R.id.field){
				Log.d(TAG, "Selected field in field list");
				scrollView.scrollToAfterAdd(theView.me.getTop());
				//Edit this note
				int index = list.indexOfChild(theView.me);
				currentField = theView.field;
				
				listener.FieldListSelectField(currentField);
				/*listNotes.removeView(noteView.me);
				View newView = inflateOpenNote(currentNote);
				listNotes.addView(newView, index);
				currentOpenNoteView = (OpenNoteView) newView.getTag();
				*/
				//TODO change to notes list
			}
		}
	};
	
	static class FieldView
    {
		TextView tvName;
		TextView tvAcres;
		RelativeLayout row1;
		RelativeLayout row2;
		RelativeLayout layField;
		Field field;
		View me;
    }
}