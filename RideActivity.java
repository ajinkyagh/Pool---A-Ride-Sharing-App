using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Gms.Maps;
using Android.Widget;
using Android.Gms.Maps.Model;
using Xamarin.Essentials;
using System.Threading.Tasks;
using Android.Text.Format;
using Android.Util;
using Android.Gms.Location.Places;
using Android.Gms.Location.Places.UI;
using MySql.Data.MySqlClient;
using System.Collections.Generic;
using System.Data;
using System.Threading;
using System.IO;


namespace Pool
{
    [Activity(Label = "RideActivity")]
    public class RideActivity : Activity, IOnMapReadyCallback
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
        TextView namedisp;
        ImageButton publish;
        ListView driverlist;
        AutoCompleteTextView locality;
        //ExpandableListView driverlist;
        string name;
        string number;
        string gender;
        string stime, etime;
        double lat, lon;
        double newlat, newlon;
        Fragment map;
        private GoogleMap GMap;
        private List<string> drivers;

        MySqlConnection con = new MySqlConnection("MySQL connection parametres");

        static string[] localities = new string[] {
  "PLACES API | PLACES",
};


        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);
            SetContentView(Resource.Layout.Ride);
            //Platform.Init(this, savedInstanceState);
            place = FindViewById<TextView>(Resource.Id.goingplace);
            //place.Click += Place_Click;
            date = FindViewById<TextView>(Resource.Id.dategoing);
            date.Click += Date_Click;
            time = FindViewById<TextView>(Resource.Id.timegoing);
            time.Click += Time_Click;
            publish = FindViewById<ImageButton>(Resource.Id.submit);
            publish.Click += Publish_Click;
            name = Intent.GetStringExtra("name");
            number = Intent.GetStringExtra("number");
            gender = Intent.GetStringExtra("gender");
            namedisp = FindViewById<TextView>(Resource.Id.greet);
            namedisp.Text = "Hello " + name;
            driverlist = FindViewById<ListView>(Resource.Id.listView1);
            locality = FindViewById<AutoCompleteTextView>(Resource.Id.autoCompleteTextView1);
            var adapterlocal = new ArrayAdapter<string>(this, Android.Resource.Layout.SimpleListItem1, localities);
            locality.Adapter = adapterlocal;
            locality.ItemClick += Locality_ItemClick;
            //SetUpMap();
            //MapBuilding25();
            //nav();
            //Create your application here
        }

        private void Locality_ItemClick(object sender, AdapterView.ItemClickEventArgs e)
        {
            driverlist.Visibility = ViewStates.Invisible;
            Locality();
        }

        private void Publish_Click(object sender, EventArgs e)
        {
            driverlist.Visibility = ViewStates.Visible;
            drivers = new List<string>();
            MySqlCommand cmd = new MySqlCommand("Select Command", con);

            try
            {
                if (con.State == ConnectionState.Closed)
                {
                    con.Open();
                    cmd.Parameters.AddWithValue("@Place", locality.Text);
                    cmd.Parameters.AddWithValue("@stime",stime);
                    cmd.Parameters.AddWithValue("@etime",etime);
                    cmd.Parameters.AddWithValue("@Date", date.Text);
                    cmd.Parameters.AddWithValue("@Gender", gender);
                    MySqlDataReader dataReader = cmd.ExecuteReader();
                    while (dataReader.Read())
                    {
                        drivers.Add("Name : " + dataReader["Name"]);
                        drivers.Add("       Number : " + dataReader["Number"]);
                        drivers.Add("       Time : " + dataReader["Time"]);
                        drivers.Add("       Price Rs : " + dataReader["Price"]);
                        var adapter = new ArrayAdapter<string>(this, Android.Resource.Layout.SimpleListItem1, drivers);
                        adapter.SetDropDownViewResource(Android.Resource.Layout.SimpleListItem1);
                        driverlist.Adapter = adapter;
                    }

                    dataReader.Close();

                }
            }
            catch (MySqlException ex)
            {
                cmd.Cancel();
            }
            finally
            {
                con.Close();
            }
        }

        private void Time_Click(object sender, EventArgs e)
        {
            TimePickerFragment frag = TimePickerFragment.NewInstance(
                 delegate (DateTime timepick)
                 {
                     time.Text = timepick.ToShortTimeString();
                     stime = timepick.AddHours(-0.5).ToShortTimeString();
                     etime = timepick.AddHours(0.5).ToShortTimeString();
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
            AutocompleteFilter filter = new AutocompleteFilter.Builder()
               .SetCountry("IN")
               .Build();

            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.ModeOverlay)
                .SetFilter(filter)
                .Build(this);

            StartActivityForResult(intent, 1);
        }

        protected override void OnActivityResult(int requestCode, [GeneratedEnum] Result resultCode, Intent data)
        {
            base.OnActivityResult(requestCode, resultCode, data);

            if (requestCode == 1)
            {
                if (resultCode == Android.App.Result.Ok)
                {
                    var places = PlaceAutocomplete.GetPlace(this, data);
                    place.Text = places.NameFormatted.ToString();
                }
            }
        }

        public override void OnRequestPermissionsResult(int requestCode, string[] permissions, [GeneratedEnum] Android.Content.PM.Permission[] grantResults)
        {
            Platform.OnRequestPermissionsResult(requestCode, permissions, grantResults);
            base.OnRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        public void nav()
        {
            Android.Net.Uri mapUri = Android.Net.Uri.Parse("google.navigation:q=" + Android.Net.Uri.Encode("kumar pacific mall&mode=l"));
            //Android.Net.Uri mapUri = Android.Net.Uri.Parse("geo:0,0?q=" + Android.Net.Uri.Encode("kumar pacific mall"));
            Intent mapIntent = new Intent(Intent.ActionView, mapUri);

            //
            mapIntent.SetPackage("com.google.android.apps.maps");
            StartActivity(mapIntent);
        }

        public async Task MapBuilding25()
        {
            var location = new Location(47.645160, -122.1306032);
            var options = new MapLaunchOptions { Name = "Microsoft Building 25" };

            await Map.OpenAsync(location, options);
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
            var placemarks = await Geocoding.GetPlacemarksAsync(newlat,newlon);
            var placemark = placemarks?.FirstOrDefault();
            if (placemark != null)
            {
                locality.Text = placemark.SubLocality + "," + placemark.PostalCode;   
            }
        }
    }
}