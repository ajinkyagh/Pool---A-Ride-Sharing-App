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
    [Activity(Label = "Pool", MainLauncher = true)]
    public class LoginActivity : Activity
    {

        Button login;
        Button register;
        EditText email;
        EditText password;
        int loggedout = 0;
        ProgressBar progress;
        string fileid = Path.Combine(System.Environment.GetFolderPath(System.Environment.SpecialFolder.LocalApplicationData), "*|txt");
        string filepass = Path.Combine(System.Environment.GetFolderPath(System.Environment.SpecialFolder.LocalApplicationData), "*|txt");
        string filename = Path.Combine(System.Environment.GetFolderPath(System.Environment.SpecialFolder.LocalApplicationData), "*|txt");
        string filegen = Path.Combine(System.Environment.GetFolderPath(System.Environment.SpecialFolder.LocalApplicationData), "*|txt");
        string isid;
        string ispass;
        string isname;
        string isgen;
        string userid;
        string usernme;
        string pass;
        string gender;

        MySqlConnection con = new MySqlConnection("MySQL Connection Parameters");
        private List<string> customer;

        public override void OnBackPressed()
        {
            if (loggedout == 1) return;

            base.OnBackPressed();
        }

        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);
            loggedout = 1;
            SetContentView(Resource.Layout.Login);
            email = FindViewById<EditText>(Resource.Id.email);
            password = FindViewById<EditText>(Resource.Id.password);
            progress = FindViewById<ProgressBar>(Resource.Id.progressBar1);
            progress.Visibility = Android.Views.ViewStates.Invisible;
            login = FindViewById<Button>(Resource.Id.loginbutton);
            login.Click += Login_Click;
            register = FindViewById<Button>(Resource.Id.registerbutton);
            register.Click += Register_Click;

            bool existid = File.Exists(fileid);
            bool existpass = File.Exists(filepass);
            if (existid == true && existpass==true)
            {
                isid = File.ReadAllText(fileid);
                ispass = File.ReadAllText(filepass);
                isname = File.ReadAllText(filename);
                isgen = File.ReadAllText(filegen);

                if (isid != "")
                {
                    Toast.MakeText(this, "Logged in : " + isname, ToastLength.Long).Show();
                    Intent intent = new Intent(this, typeof(SelectActivity));
                    intent.PutExtra("password", ispass);
                    intent.PutExtra("name", isname);
                    intent.PutExtra("number", isid);
                    intent.PutExtra("gender", isgen);
                    StartActivity(intent);
                    loggedout = 0;
                }

            }


            // Create your application here
        }

        private void Login_Click(object sender, EventArgs e)
        {
            progress.Visibility = Android.Views.ViewStates.Visible;
            customer = new List<string>();
            try
            {
                if (con.State == ConnectionState.Closed)
                {
                    con.Open();

                    MySqlCommand cmd = new MySqlCommand("Select Command", con);
                    cmd.Parameters.AddWithValue("@Phone", email.Text);
                    cmd.Parameters.AddWithValue("@Password", password.Text);

                    MySqlDataReader dataReader = cmd.ExecuteReader();
                    while (dataReader.Read())
                    {
                        usernme = dataReader["Name"] + "";
                        userid = dataReader["Phone"] + "";
                        pass = dataReader["Password"] + "";
                        gender = dataReader["Gender"] + "";
                    }
                    dataReader.Close();
                }
            }
            catch (MySqlException ex)
            {
                Toast.MakeText(this, ex.ToString(), ToastLength.Long).Show();
            }
            finally
            {
                con.Close();
            }

            if (email.Text == "")
            {
                Toast.MakeText(this, "Enter a valid ID.!", ToastLength.Long).Show();
            }
            else
            {
                if (email.Text == userid && password.Text==pass)
                {
                    File.WriteAllText(fileid, userid);
                    File.WriteAllText(filepass, pass);
                    File.WriteAllText(filename, usernme);
                    File.WriteAllText(filegen, gender);

                    Toast.MakeText(this, "Logged in : " + usernme, ToastLength.Long).Show();
                    Intent intent = new Intent(this, typeof(SelectActivity));
                    intent.PutExtra("name", usernme);
                    intent.PutExtra("number", userid);
                    intent.PutExtra("gender", gender);
                    StartActivity(intent);
                    loggedout = 0;
                }
                else
                {
                    Toast.MakeText(this, "Username not valid.!", ToastLength.Long).Show();
                    email.Text = "";
                }
            }
        }

        private void Register_Click(object sender, EventArgs e)
        {
            StartActivity(typeof(RegisterActivity));
        }
    }
}