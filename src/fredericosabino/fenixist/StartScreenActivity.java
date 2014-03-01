package fredericosabino.fenixist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import fredericosabino.fenixist.R;

public class StartScreenActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_screen);
	}
	
	public void goToPage(View view) {
		Intent intent = new Intent(this, OAuthActivity.class);
		startActivity(intent);
	}
}
