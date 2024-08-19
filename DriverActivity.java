using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Gms.Location.Places.UI;
using Android.Gms.Location.Places;
using Android.Gms;
using Android.Gms.Location;
using Android.Widget;
using Android.Util;
using Android.Text.Format;
using MySql.Data.MySqlClient;
using Plugin.LocalNotifications;
using Google.Places;
using Xamarin.Essentials;
using System.Collections.Generic;
using System.Data;
using System.Threading;
using System.IO;
using System.Threading.Tasks;
using Place = Google.Places.Place;
using Android.Gms.Maps;
using Android.Gms.Maps.Model;

namespace Pool
{
    [Activity(Label = "DriverActivity")]
    public class DriverActivity : Activity, IOnMapReadyCallback
    {

        public class DatePickerFragment : DialogFragment,
                                  DatePickerDialog.IOnDateSetListener
        {
            // TAG can be any string of your choice.
            public static readonly string TAG = "X:" + typeof(DatePickerFragment).Name.ToUpper();

            // Initialize this value to prevent NullReferenceExceptions.
            Action<DateTime> _dateSelectedHandler = delegate { };

            public static DatePickerFragment NewInstance(Action<DateTime> onDateSelected)
            {
                DatePickerFragment frag = new DatePickerFragment();
                frag._dateSelectedHandler = onDateSelected;
                return frag;
            }

            public override Dialog OnCreateDialog(Bundle savedInstanceState)
            {
                DateTime currently = DateTime.Now;
                DatePickerDialog dialog = new DatePickerDialog(Activity,
                                                               this,
                                                               currently.Year,
                                                               currently.Month - 1,
                                                               currently.Day);
                return dialog;
            }

            public void OnDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                // Note: monthOfYear is a value between 0 and 11, not 1 and 12!
                DateTime selectedDate = new DateTime(year, monthOfYear + 1, dayOfMonth);
                Log.Debug(TAG, selectedDate.ToLongDateString());
                _dateSelectedHandler(selectedDate);
            }
        }

        public class TimePickerFragment : DialogFragment, TimePickerDialog.IOnTimeSetListener
        {
            public static readonly string TAG = "MyTimePickerFragment";
            Action<DateTime> timeSelectedHandler = delegate { };

            public static TimePickerFragment NewInstance(Action<DateTime> onTimeSelected)
            {
                TimePickerFragment frag = new TimePickerFragment();
                frag.timeSelectedHandler = onTimeSelected;
                return frag;
            }

            public override Dialog OnCreateDialog(Bundle savedInstanceState)
            {
                DateTime currentTime = DateTime.Now;
                bool is24HourFormat = DateFormat.Is24HourFormat(Activity);
                TimePickerDialog dialog = new TimePickerDialog
                    (Activity, this, currentTime.Hour, currentTime.Minute, is24HourFormat);
                return dialog;
            }

            public void OnTimeSet(TimePicker view, int hourOfDay, int minute)
            {
                DateTime currentTime = DateTime.Now;
                DateTime selectedTime = new DateTime(currentTime.Year, currentTime.Month, currentTime.Day, hourOfDay, minute, 0);
                Log.Debug(TAG, selectedTime.ToLongTimeString());
                timeSelectedHandler(selectedTime);
            }
        }

        TextView place;
        TextView date;
        TextView time;
        EditText price;
        TextView namedisp;
        ImageButton publish;
        AutoCompleteTextView locality;
        string name;
        string number;
        string gender;
        double lat, lon;
        double newlat, newlon;
        private GoogleMap GMap;
        MySqlConnection con = new MySqlConnection("MySQL Conncetion Parameters");

        static string[] localities = new string[] {
  "PLACES API | PLACES",

};

        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);
            SetContentView(Resource.Layout.Driver);
            place = FindViewById<TextView>(Resource.Id.place);
            //place.Click += Place_Click;
            date = FindViewById<TextView>(Resource.Id.date);
            date.Click += Date_Click;
            time = FindViewById<TextView>(Resource.Id.textView2);
            time.Click += Time_Click;
            publish = FindViewById<ImageButton>(Resource.Id.imagebutton);
            publish.Click += Publish_Click;
            price = FindViewById<EditText>(Resource.Id.editText1);
            name = Intent.GetStringExtra("name");
            number = Intent.GetStringExtra("number");
            gender = Intent.GetStringExtra("gender");
            namedisp = FindViewById<TextView>(Resource.Id.textView3);
            namedisp.Text = "Hello " + name;
            locality = FindViewById<AutoCompleteTextView>(Resource.Id.autoCompleteTextView1);
            var adapterlocal = new ArrayAdapter<string>(this, Android.Resource.Layout.SimpleListItem1, localities);
            locality.Adapter = adapterlocal;
            locality.ItemClick += Locality_ItemClick;
            /*if(!PlacesApi.IsInitialized)
            {
                PlacesApi.Initialize(this, "AIzaSyA6TZymgN00bhXlvH93giF7wkPdxXz-_1s");
            }*/
            // Create your application here

        }

        private void Locality_ItemClick(object sender, AdapterView.ItemClickEventArgs e)
        {
            Locality();
        }

        private void Publish_Click(object sender, EventArgs e)
        {
            if (place.Text == "" || date.Text == "" || time.Text == "")
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

                        cmd.Parameters.AddWithValue("@Name", name);
                        cmd.Parameters.AddWithValue("@Number", number);
                        cmd.Parameters.AddWithValue("@Date", date.Text);
                        cmd.Parameters.AddWithValue("@Time", time.Text);
                        cmd.Parameters.AddWithValue("@Place", locality.Text);
                        cmd.Parameters.AddWithValue("@Price", price.Text);
                        cmd.Parameters.AddWithValue("@Gender", gender);
                        cmd.ExecuteNonQuery();
                        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                        AlertDialog alert = dialog.Create();
                        alert.SetTitle("Status");
                        alert.SetMessage("Published Successfully!");
                        alert.SetButton("OK", (c, ev) =>
                        { });
                        //CrossLocalNotifications.Current.Show("hello", "try");
                        alert.Show();
                        //StartActivity(typeof(LoginActivity));
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

        private void Time_Click(object sender, EventArgs e)
        {
            TimePickerFragment frag = TimePickerFragment.NewInstance(
                delegate (DateTime timepick)
                {
                    time.Text = timepick.ToShortTimeString();
                });

            frag.Show(FragmentManager, TimePickerFragment.TAG);
        }

        private void Date_Click(object sender, EventArgs e)
        {
            DatePickerFragment frag = DatePickerFragment.NewInstance(delegate (DateTime time)
            {
                int d;
                int m;
                int y;
                d = time.Day;
                m = time.Month;
                y = time.Year;
                date.Text = y + "/" + m + "/" + d;
            });
            frag.Show(FragmentManager, DatePickerFragment.TAG);
        }



        /*private void Place_Click(object sender, EventArgs e)
        {
            List<Place.Field> fields = new List<Place.Field>();
            fields.Add(Place.Field.Address);
            fields.Add(Place.Field.Name);
            fields.Add(Place.Field.Id);
            fields.Add(Place.Field.LatLng);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.Overlay, fields)
                .SetCountry("IN")
                .Build(this);
            StartActivityForResult(intent, 0);
            AutocompleteFilter filter = new AutocompleteFilter.Builder()
                .SetCountry("IN")
                .SetTypeFilter(AutocompleteFilter.TypeFilterNone)
                .Build();

            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.ModeOverlay)
                .SetFilter(filter)
                .Build(this);
            //AIzaSyA4z1nMNMtAwP47zWC0Xd7_gt67A3EPCMI
            //AIzaSyCXPLX5HPYK3x7XUwAfaSiHaf60t8Y7jdo Working
            //AlzaSyBpBjZCW61j0r9KbfZOymvpzDziJvaJeu4
            //AIZASYCM5UEIFQdiXdlMaqUK020QX3wulC1éTE
            //AlzaSyCArd2hjQ8xs7y8pwstUcsb2FDbEBslH30
            //AIzaSyA6Y2BX-uF7ttWvfucY47nRsIUuz_LpJJE
            //E5:F1:2B:D7:BE:F7:06:19:79:FE:FA:5B:39:3E:09:09:F6:B4:23:3B

            StartActivityForResult(intent, 1);
        }*/

        /*protected override void OnActivityResult(int requestCode,[GeneratedEnum] Result resultCode,Intent data)
        {
            base.OnActivityResult(requestCode, resultCode, data);
            var places = Autocomplete.GetPlaceFromIntent(data);
            place.Text = places.Address;
        }*/

        async Task Locality()
        {
            try
            {
                var address = locality.Text + " pune";
                var locations = await Geocoding.GetLocationsAsync(address);

                var location = locations?.FirstOrDefault();
                if (location != null)
                {
                    lat = location.Latitude;
                    lon = location.Longitude;
                    SetUpMap();
                }
            }
            catch (FeatureNotSupportedException fnsEx)
            {
                // Feature not supported on device
            }
            catch (Exception ex)
            {
                // Handle exception that may have occurred in geocoding
            }
        }


        private void SetUpMap()
        {
            if (GMap == null)
            {
                var mapFragment = (MapFragment)FragmentManager.FindFragmentById(Resource.Id.googlemap);
                mapFragment.GetMapAsync(this);
            }
        }

        public void OnMapReady(GoogleMap googleMap)
        {
            //googleMap.MapType = GoogleMap.MapTypeHybrid;
            LatLng location = new LatLng(lat, lon);
            CameraPosition.Builder builder = CameraPosition.InvokeBuilder();
            builder.Target(location);
            builder.Zoom(16);
            CameraPosition cameraPosition = builder.Build();
            CameraUpdate cameraUpdate = CameraUpdateFactory.NewCameraPosition(cameraPosition);
            googleMap.MoveCamera(cameraUpdate);
            MarkerOptions marker = new MarkerOptions()
            .SetPosition(new LatLng(lat, lon))
            .SetTitle(locality.Text)
            .Draggable(true);
            googleMap.AddMarker(marker);
            googleMap.MarkerDrag += GoogleMap_MarkerDrag;
        }


        private void GoogleMap_MarkerDrag(object sender, GoogleMap.MarkerDragEventArgs e)
        {
            newlat = e.Marker.Position.Latitude;
            newlon = e.Marker.Position.Longitude;
            newloc();
        }

        async Task newloc()
        {
            var placemarks = await Geocoding.GetPlacemarksAsync(newlat, newlon);
            var placemark = placemarks?.FirstOrDefault();
            if (placemark != null)
            {
                locality.Text = placemark.SubLocality + "," + placemark.PostalCode;
            }
        }

        /*protected override void OnActivityResult(int requestCode, [GeneratedEnum] Result resultCode, Intent data)
        {
            if (requestCode == 1)
            {
                if (resultCode == Android.App.Result.Ok)
                {
                    var places = PlaceAutocomplete.GetPlace(this, data);
                    place.Text = places.NameFormatted.ToString();
                    Locality();
                }
            }
        }*/
    }

}