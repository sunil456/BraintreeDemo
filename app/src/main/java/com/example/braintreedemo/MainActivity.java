package com.example.braintreedemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.HttpClient;
import com.braintreepayments.api.models.PaymentMethodNonce;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1234;
    final String API_GET_TOKEN = "http://10.0.2.2:8081/braintree/main.php";
   final String API_CHECK_OUT = "http://10.0.2.2:8081/braintree/checkout.php";
//    String API_GET_TOKEN = "http://localhost:8081/braintree/main.php";


    String token,amount;
    HashMap<String,String> paramsHash;

    Button btn_pay;
    EditText edt_amount;
    LinearLayout waiting_group , payment_group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        payment_group = findViewById(R.id.payment_group);
        waiting_group = findViewById(R.id.waiting_group);

        btn_pay = findViewById(R.id.btn_pay);
        edt_amount = findViewById(R.id.edt_amount);

        new getToken().execute();

//        Event
        btn_pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPayment();
            }
        });
    }

    private void submitPayment() {
        DropInRequest dropInRequest = new DropInRequest().clientToken(token);
//        startActivity(dropInRequest.getIntent(this) , REQUEST_CODE);
        startActivityForResult(dropInRequest.getIntent(this) , REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == REQUEST_CODE)
        {
            if (resultCode == RESULT_OK)
            {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                PaymentMethodNonce nonce = result.getPaymentMethodNonce();

                String strNonce = nonce.getNonce();

                if (!edt_amount.getText().toString().isEmpty())
                {
                    amount = edt_amount.getText().toString();
                    paramsHash = new HashMap<>();
                    paramsHash.put("amount" , amount);
                    paramsHash.put("nonce" , strNonce);

                    sendPayments();
                }
                else
                {
                    Toast.makeText(this, "Please enter valid amount", Toast.LENGTH_SHORT).show();
                }
            }
            else if (resultCode == RESULT_CANCELED)
            {
                Toast.makeText(this, "User Cancel", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                Log.d("PAYMENT_ERROR", error.toString());
            }
        }
    }

    private void sendPayments() {
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, API_CHECK_OUT, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response.toString().contains("Successful"))
                {
                    Toast.makeText(MainActivity.this, "Transaction successful!", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Transaction Failed!", Toast.LENGTH_SHORT).show();

                }
                Log.d("PAYMENT_LOG", response.toString());

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("PAYMENT_ERROR", error.toString());

            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                if(paramsHash == null)
                {
                    return  null;
                }
                Map<String , String> params = new HashMap<>();

                for (String key:params.keySet())
                {
                    params.put(key , paramsHash.get(key));
                }
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("Content-Type" , "application/x-www-form-urlencoded");

                return params;
            }
        };

        queue.add(stringRequest);
    }

    private class getToken extends AsyncTask{

        ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(MainActivity.this , android.R.style.Theme_DeviceDefault_Dialog);
            mDialog.setCancelable(false);
            mDialog.setMessage("Please wait");
            mDialog.show();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            HttpClient client = new HttpClient();
            client.get(API_GET_TOKEN, new HttpResponseCallback() {
                @Override
                public void success(final String responseBody) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            here we have to hide group waiting
                            waiting_group.setVisibility(View.GONE);
//                            now show group payment
                            payment_group.setVisibility(View.VISIBLE);

//                            set token
                            token = responseBody;
                        }
                    });
                }

                @Override
                public void failure(Exception exception) {
                    Log.d("PAYMENT_GETWAY_ERROR", "failure: " + exception.toString());
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            mDialog.dismiss();
        }
    }
}
