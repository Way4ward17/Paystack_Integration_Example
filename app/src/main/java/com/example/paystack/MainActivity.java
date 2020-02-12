package com.example.paystack;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;

import co.paystack.android.Paystack;
import co.paystack.android.PaystackSdk;
import co.paystack.android.Transaction;
import co.paystack.android.exceptions.ExpiredAccessCodeException;
import co.paystack.android.model.Card;
import co.paystack.android.model.Charge;

public class MainActivity extends AppCompatActivity {
//CREATE A PAYSTACK ACCOUNT AND PAST UR TEST KEY HERE... THE BELOW ONE IS INCORRECT
    String paystack_public_key = "pk_test_4f143f9bbbbd9201c0ae63ea3dae2a7";

    EditText mEditCardNum;
    EditText mEditCVC;
    EditText mEditExpiryMonth;
    EditText mEditExpiryYear;

    TextView mTextError;
    TextView mTextBackendMessage;

    ProgressDialog dialog;
    private TextView mTextReference;
    private Charge charge;
    private Transaction transaction;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (BuildConfig.DEBUG && (paystack_public_key.equals(""))) {
            throw new AssertionError("Please set a public key before running the sample");
        }

        PaystackSdk.setPublicKey(paystack_public_key);

        mEditCardNum = findViewById(R.id.edit_card_number);
        mEditCVC = findViewById(R.id.edit_cvc);
        mEditExpiryMonth = findViewById(R.id.edit_expiry_month);
        mEditExpiryYear = findViewById(R.id.edit_expiry_year);


mEditCardNum.setText("5060666666666666666");
        Button mButtonPerformTransaction = findViewById(R.id.button_perform_transaction);
        Button mButtonPerformLocalTransaction = findViewById(R.id.button_perform_local_transaction);

        mTextError = findViewById(R.id.textview_error);
        mTextBackendMessage = findViewById(R.id.textview_backend_message);
        mTextReference = findViewById(R.id.textview_reference);

        //initialize sdk
        PaystackSdk.initialize(getApplicationContext());

        //set click listener
        mButtonPerformLocalTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    startAFreshCharge(true);
                } catch (Exception e) {
                    MainActivity.this.mTextError.setText(String.format("An error occurred while charging card: %s %s", e.getClass().getSimpleName(), e.getMessage()));

                }
            }
        });
    }

    private void startAFreshCharge(boolean local) {
        // initialize the charge
        charge = new Charge();
        charge.setCard(loadCardFromForm());

        dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Performing transaction... please wait");
        dialog.show();

        if (local) {
            // Set transaction params directly in app (note that these params
            // are only used if an access_code is not set. In debug mode,
            // setting them after setting an access code would throw an exception
//Any amount is multiplied by 100
            charge.setAmount(2000);
            charge.setEmail("customer@email.com");
            charge.setReference("ChargedFromAndroid_" + Calendar.getInstance().getTimeInMillis());
            try {
                charge.putCustomField("Charged From", "Android SDK");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            chargeCard();
        }
    }

    /**
     * Method to validate the form, and set errors on the edittexts.
     */
    private Card loadCardFromForm() {
        //validate fields
        Card card;

        String cardNum = mEditCardNum.getText().toString().trim();

        //build card object with ONLY the number, update the other fields later
        card = new Card.Builder(cardNum, 0, 0, "").build();
        String cvc = mEditCVC.getText().toString().trim();
        //update the cvc field of the card
        card.setCvc(cvc);

        //validate expiry month;
        String sMonth = mEditExpiryMonth.getText().toString().trim();
        int month = 0;
        try {
            month = Integer.parseInt(sMonth);
        } catch (Exception ignored) {
        }

        card.setExpiryMonth(month);

        String sYear = mEditExpiryYear.getText().toString().trim();
        int year = 0;
        try {
            year = Integer.parseInt(sYear);
        } catch (Exception ignored) {
        }
        card.setExpiryYear(year);

        return card;
    }

    @Override
    public void onPause() {
        super.onPause();

        if ((dialog != null) && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
    }

    private void chargeCard() {
        transaction = null;
        PaystackSdk.chargeCard(MainActivity.this, charge, new Paystack.TransactionCallback() {
            // This is called only after transaction is successful
            @Override
            public void onSuccess(Transaction transaction) {
                dismissDialog();

                Toast.makeText(MainActivity.this, "SUCESS SUCESS", Toast.LENGTH_LONG).show();
                updateTextViews();
                mTextError.setText(" ");
            }

            // This is called only before requesting OTP
            // Save reference so you may send to server if
            // error occurs with OTP
            // No need to dismiss dialog
            @Override
            public void beforeValidate(Transaction transaction) {
                MainActivity.this.transaction = transaction;
                Toast.makeText(MainActivity.this, transaction.getReference(), Toast.LENGTH_LONG).show();
                updateTextViews();
            }

            @Override
            public void onError(Throwable error, Transaction transaction) {
                // If an access code has expired, simply ask your server for a new one
                // and restart the charge instead of displaying error
                MainActivity.this.transaction = transaction;
                if (error instanceof ExpiredAccessCodeException) {
                    MainActivity.this.startAFreshCharge(false);
                    MainActivity.this.chargeCard();
                    return;
                }

                dismissDialog();

                if (transaction.getReference() != null) {
                    Toast.makeText(MainActivity.this, transaction.getReference() + " concluded with error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    mTextError.setText(String.format("%s  concluded with error: %s %s", transaction.getReference(), error.getClass().getSimpleName(), error.getMessage()));
                   } else {
                    Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    mTextError.setText(String.format("Error: %s %s", error.getClass().getSimpleName(), error.getMessage()));
                }
                updateTextViews();
            }

        });
    }

    private void dismissDialog() {
        if ((dialog != null) && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void updateTextViews() {
        if (transaction.getReference() != null) {
            mTextReference.setText(String.format("Reference: %s", transaction.getReference()));
        } else {
            mTextReference.setText("No transaction");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private boolean isEmpty(String s) {
        return s == null || s.length() < 1;
    }









}
