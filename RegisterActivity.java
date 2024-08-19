using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using MySql.Data.MySqlClient;
using System.Collections.Generic;
using System.Data;
using System.Threading;
using System.IO;


namespace Pool
{
    [Activity(Label = "RegisterActivity")]
    public class RegisterActivity : Activity
    {
        EditText name;
        EditText email;
        EditText number;
        EditText pass;
        RadioButton male;
        RadioButton female;
        Button reg;
        string gen;
        MySqlConnection con = new MySqlConnection("MySQL Connection Parameters");

        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);
            SetContentView(Resource.Layout.Register);
            name = FindViewById<EditText>(Resource.Id.Name);
            email = FindViewById<EditText>(Resource.Id.Email);
            pass = FindViewById<EditText>(Resource.Id.Pass);
            number = FindViewById<EditText>(Resource.Id.Number);
            female = FindViewById<RadioButton>(Resource.Id.radioButton1);
            male = FindViewById<RadioButton>(Resource.Id.radioButton2);
            male.Click += RadioButtonClick;
            female.Click += RadioButtonClick;
            reg = FindViewById<Button>(Resource.Id.button1);
            reg.Click += Reg_Click;
            // Create your application here
        }

        private void RadioButtonClick(object sender, EventArgs e)
        {
            RadioButton rb = (RadioButton)sender;
            gen = rb.Text;
            Toast.MakeText(this, gen, ToastLength.Short).Show();
        }

        private void Reg_Click(object sender, EventArgs e)
        {

            if (name.Text == "" || email.Text == "" || number.Text == "")
            {
                Toast.MakeText(this, "Enter all information", ToastLength.Long).Show();
            }

            else
            {
                MySqlCommand cmd = new MySqlCommand("Insert Command", con);
                try
                {
                    if (con.State == ConnectionState.Closed)
                    {
                        con.Open();

                        cmd.Parameters.AddWithValue("@Name", name.Text);
                        cmd.Parameters.AddWithValue("@Email", email.Text);
                        cmd.Parameters.AddWithValue("@Phone", number.Text);
                        cmd.Parameters.AddWithValue("@Password", pass.Text);
                        if (gen == "Female")
                        {
                            cmd.Parameters.AddWithValue("@Gender", "Female");
                        }
                        else
                        {
                            cmd.Parameters.AddWithValue("@Gender", "Male");
                        }
                        cmd.ExecuteNonQuery();
                        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                        AlertDialog alert = dialog.Create();
                        alert.SetTitle("Status");
                        alert.SetMessage("Registered Successfully!");
                        alert.SetButton("OK", (c, ev) =>
                        { });
                        alert.Show();
                        StartActivity(typeof(LoginActivity));
                    }
                }
                catch (MySqlException ex)
                {
                    cmd.Cancel();
                    Toast.MakeText(this, "error occured.!", ToastLength.Long).Show();
                }
                finally
                {
                    con.Close();
                }
            }
        }

        private void Submit_Click(object sender, EventArgs e)
        {

        }
    }
}