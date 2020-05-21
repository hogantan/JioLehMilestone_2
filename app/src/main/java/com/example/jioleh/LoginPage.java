package com.example.jioleh;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginPage extends AppCompatActivity {
    private TextInputLayout email;
    private TextInputLayout password;
    private Button login;
    private TextView forgotpassword;

    private ProgressDialog progressBar;

    private FirebaseAuth database;
    private FirebaseFirestore fireStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);

        initialise();

        login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkUser(email.getEditText().getText().toString(), password.getEditText().getText().toString());
                }
            });

        forgotpassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent nextActivity = new Intent(LoginPage.this, ForgotPasswordPage.class);
                    startActivity(nextActivity);
                }
        });
    }

    private void initialise() {
        initialiseActionBar();
        progressBar = new ProgressDialog(LoginPage.this);
        email = findViewById(R.id.tilLoginEmail);
        password = findViewById(R.id.tilLoginPassword);
        login = findViewById(R.id.btnLoginSignIn);
        forgotpassword = findViewById(R.id.tvForgotPasswordLogin);
        database = FirebaseAuth.getInstance();
    }

    //Action Bar Settings
    private void initialiseActionBar() {
        ActionBar top_bar = getSupportActionBar();

        //Setting background colour
        ColorDrawable light_green = new ColorDrawable(Color.parseColor("#00ffce"));
        top_bar.setBackgroundDrawable(light_green);

        //Setting Title text
        top_bar.setTitle(Html.fromHtml("<font color='#202124'>Login </font>"));

        //Setting Top left logo
        final Drawable upArrow =  ContextCompat.getDrawable(LoginPage.this, R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(ContextCompat.getColor(LoginPage.this, R.color.baseBlack), PorterDuff.Mode.SRC_ATOP);
        LoginPage.this.getSupportActionBar().setHomeAsUpIndicator(upArrow);
    }

    private void checkUser(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginPage.this,
                    "Please fill in all required fields", Toast.LENGTH_SHORT).show();
        } else {

            //Setting Details of Loading Screen
            progressBar.setTitle("Logging in");
            progressBar.setMessage("Please wait while we check your credentials");
            progressBar.setCanceledOnTouchOutside(false);
            progressBar.show();

            database.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        progressBar.dismiss();
                        Toast.makeText(LoginPage.this,
                                "Login Successful", Toast.LENGTH_SHORT).show();
                        checkEmailVerification();
                    } else if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                        progressBar.dismiss();
                        Toast.makeText(LoginPage.this,
                                "User does not exist",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        progressBar.dismiss();
                        Toast.makeText(LoginPage.this,
                                "Email address or password is incorrect",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void checkEmailVerification() {
        FirebaseUser currentUser = database.getCurrentUser();
        boolean isVerified = currentUser.isEmailVerified();
        String userID = currentUser.getUid();
        if (isVerified) {
            Intent nextActivity = new Intent(LoginPage.this, PostLoginPage.class);
            nextActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);            startActivity(nextActivity);
            finish();
        } else {
            Toast.makeText(LoginPage.this,
                    "Account is not verified, please check email",
                    Toast.LENGTH_SHORT).show();
            database.signOut(); //sign out unverified user
        }
    }
}