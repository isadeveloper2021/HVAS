package com.example.hvas;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hvas.adapter.ListDataAdapter;
import com.example.hvas.databinding.ActivityMainBinding;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    String[] items = {"PM 10", "PM 2.5", "TSP"};
    ArrayAdapter<String> adapterItems;
    BluetoothSPP bluetooth;
    String sensorData, loadPump, startSampling, stopSampling, showList, print, loadSet, saveSet;
    String menuCommand, upCommand, downCommand, backCommand, enterCommand;
    List<String> dataList = new ArrayList<>();
    Integer dataIndex;
    boolean isSamplingRunning = false;
    int REQUEST_LOCATION = 88;

    /*Permission*/
    private static final String[] BLUETOOTH_ABOVE_29_PERMISSION = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
    };
    private static final String[] BLUETOOTH_BELOW_30_PERMISSION = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    };
    private static final int BLUETOOTH_PERMISSIONS_ABOVE_29_REQUEST_CODE = 1;
    private static final int BLUETOOTH_PERMISSIONS_BELOW_30_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkBluetoothAvailability();

        adapterItems = new ArrayAdapter<>(this, R.layout.list_item, items);
        binding.autoTvType.setAdapter(adapterItems);

        binding.autoTvType.setOnItemClickListener((parent, view, position, id) -> {
            String item = parent.getItemAtPosition(position).toString();
            Toast.makeText(getApplicationContext(), "" + item, Toast.LENGTH_SHORT).show();
        });

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(() -> {
                            long date = System.currentTimeMillis();
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy\nhh:mm:ss a");
                            String dateString = sdf.format(date);
                            binding.tvDate.setText(dateString);
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
        address();
        getLocation();
        requestLocation();

        initListener();
    }

    private void initListener() {

        bluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                binding.btnConnect.setText(getString(R.string.two_string_with_white_space_pattern, "Connected to", name));
                Toast.makeText(MainActivity.this, R.string.connected, Toast.LENGTH_SHORT).show();
            }

            public void onDeviceDisconnected() {
                binding.btnConnect.setText(R.string.connection_lost);
                Toast.makeText(MainActivity.this, R.string.connection_lost, Toast.LENGTH_SHORT).show();

            }

            public void onDeviceConnectionFailed() {
                binding.btnConnect.setText(R.string.unable_to_connect);
                Toast.makeText(MainActivity.this, R.string.unable_to_connect, Toast.LENGTH_SHORT).show();

            }
        });

        binding.btnConnect.setOnClickListener(v -> {
            if (bluetooth.getServiceState() == BluetoothState.STATE_CONNECTED) {
                bluetooth.disconnect();
            } else {
                Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
            }
        });

        /*Command*/
        sensorData = "000A"; // 000A
        loadPump = "B"; // Hapus
        startSampling = "000C"; //000C
        stopSampling = "STOP";
        showList = "000D"; //000D
        print = "000E"; //000E[index]
        loadSet = "LOAD"; // LOAD
        saveSet = "G"; // Hapus

        /*Device Navigation Command*/
        menuCommand = "MEN";
        upCommand = "UP";
        downCommand = "DOWN";
        backCommand = "CAN";
        enterCommand = "ENT";

        binding.cvSensorData.setOnClickListener(v -> {
            if (bluetooth.getServiceState() != -1) {
                bluetooth.send(sensorData, false);
                Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
                bluetooth.setOnDataReceivedListener((data, message) -> {
                    String receivedData = new String(data);
                    String[] bagi = receivedData.split(",");
                    binding.tvTemp.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 0), "C"));
                    binding.tvHumidity.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 1), "RH"));
                    binding.tvPressure.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 2), "hPa"));
                    binding.tvBattery.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 3), "V"));
                    Toast.makeText(getApplicationContext(), "Data Received", Toast.LENGTH_SHORT).show();
                });
            } else {
                checkBluetoothAvailability();
            }
        });

        /*binding.btnLoadPump.setOnClickListener(v -> {
            binding.btnStartSampling.setVisibility(View.VISIBLE);
            bluetooth.send(loadPump, false);
            Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
            bluetooth.setOnDataReceivedListener((data, message) -> {
                String receivedData = new String(data);
                String[] bagi = receivedData.split(",");
                binding.tvTimeSampling.setText(getString(R.string.two_string_with_white_space_pattern, bagi[0], "Minutes"));
                Toast.makeText(getApplicationContext(), "Data Received", Toast.LENGTH_SHORT).show();
            });
        });*/

        /*binding.btnStartSampling.setOnClickListener(v -> {
            bluetooth.send(startSampling, false);
            Toast.makeText(MainActivity.this, "Data Sent", Toast.LENGTH_SHORT).show();
            bluetooth.setOnDataReceivedListener((data, message) -> {
                String receivedData = new String(data);
                String[] bagi = receivedData.split(",");
                binding.tvMonitorTemp.setText(getString(R.string.two_string_with_white_space_pattern, bagi[0], "C"));
                binding.tvMonitorHumidity.setText(getString(R.string.two_string_with_white_space_pattern, bagi[1], "RH"));
                binding.tvMonitorPressure.setText(getString(R.string.two_string_with_white_space_pattern, bagi[2], "hPa"));
                binding.tvMonitorBattery.setText(getString(R.string.two_string_with_white_space_pattern, bagi[3], "V"));
                binding.tvMonitorTimeSampling.setText(getString(R.string.two_string_with_white_space_pattern, bagi[4], "H"));
                binding.tvMonitorMinutesSampling.setText(getString(R.string.two_string_with_white_space_pattern, bagi[5], "Minutes"));
                binding.tvMonitorSecondsSampling.setText(getString(R.string.two_string_with_white_space_pattern, bagi[6], "Seconds"));
                binding.tvMonitorLongSamplingTime.setText(getString(R.string.two_string_with_white_space_pattern, bagi[7], "Minutes"));
                Toast.makeText(getApplicationContext(), "Data Received", Toast.LENGTH_SHORT).show();

                if (binding.tvMonitorMinutesSampling != binding.tvMonitorLongSamplingTime) {
                    Toast.makeText(MainActivity.this, "Sampling On Process", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        Thread.sleep(1000);
                        Toast.makeText(MainActivity.this, "Sampling Done", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        });*/

        binding.cvSampling.setOnClickListener(v -> {
            if (isSamplingRunning) {
                if (bluetooth.getServiceState() != -1) {
                    bluetooth.send(stopSampling, false);
                    isSamplingRunning = false;
                    binding.tvSamplingCondition.setText(getString(R.string.start_sampling));
                    binding.cvSampling.setCardBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.card_off));
                    Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
                    bluetooth.setOnDataReceivedListener((data, message) -> {
                        String receivedData = new String(data);
                        String[] bagi = receivedData.split(",");
                        binding.tvTimeSampling.setText(getString(R.string.two_string_with_white_space_pattern, bagi[0], "Minutes"));
                        Toast.makeText(getApplicationContext(), "Data Received", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    checkBluetoothAvailability();
                }
            } else {
                if (bluetooth.getServiceState() != -1) {
                    bluetooth.send(startSampling, false);
                    Toast.makeText(MainActivity.this, "Data Sent", Toast.LENGTH_SHORT).show();
                    isSamplingRunning = true;
                    binding.tvSamplingCondition.setText(getString(R.string.stop_sampling));
                    binding.cvSampling.setCardBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.card_on));
                    bluetooth.setOnDataReceivedListener((data, message) -> {
                        String receivedData = new String(data);
                        String[] bagi = receivedData.split(",");
                        binding.tvMonitorTemp.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 0), "C"));
                        binding.tvMonitorHumidity.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 1), "RH"));
                        binding.tvMonitorPressure.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 2), "hPa"));
                        binding.tvMonitorBattery.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 3), "V"));
                        binding.tvMonitorTimeSampling.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 4), "H"));
                        binding.tvMonitorMinutesSampling.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 5), "Minutes"));
                        binding.tvMonitorSecondsSampling.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 6), "Seconds"));
                        binding.tvMonitorLongSamplingTime.setText(getString(R.string.two_string_with_white_space_pattern, checkIfArrayIsOutOfBond(bagi, 7), "Minutes"));
                        Toast.makeText(getApplicationContext(), "Data Received", Toast.LENGTH_SHORT).show();

                        if (binding.tvMonitorMinutesSampling != binding.tvMonitorLongSamplingTime) {
                            Toast.makeText(MainActivity.this, "Sampling On Process", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                Thread.sleep(1000);
                                Toast.makeText(MainActivity.this, "Sampling Done", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    checkBluetoothAvailability();
                }
            }

        });

        binding.cvPrintData.setOnClickListener(v -> {
            binding.btnShowList.setVisibility(View.VISIBLE);
            Toast.makeText(getApplicationContext(), "Harap menekan tombol show list", Toast.LENGTH_SHORT).show();
        });

        binding.btnShowList.setOnClickListener(v -> {
            if (bluetooth.getServiceState() != -1) {
                bluetooth.send(showList, false);
                Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
            } else {
                checkBluetoothAvailability();
            }

            /*String receivedData = "0.05131141.TXT, 1.05131219.TXT, 2.05131222.TXT";
            dataList.clear();
            String[] dataSplitted = receivedData.split(",");
            dataList.addAll(Arrays.asList(dataSplitted));
            ListDataAdapter listDataAdapter = new ListDataAdapter(dataList, (data, index) -> {
                dataIndex = index;
                binding.etPrintNumber.setText(data);
            });
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this, RecyclerView.HORIZONTAL, false);
            binding.rvListData.setLayoutManager(linearLayoutManager);
            binding.rvListData.setAdapter(listDataAdapter);*/

            bluetooth.setOnDataReceivedListener((data, message) -> {
                String receivedData = new String(data);
                dataList.clear();
                String[] dataSplitted = receivedData.split(",");
                dataList.addAll(Arrays.asList(dataSplitted));
                ListDataAdapter listDataAdapter = new ListDataAdapter(dataList, (dataSelected, index) -> {
                    dataIndex = index;
                    binding.etPrintNumber.setText(dataSelected);
                });
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this, RecyclerView.HORIZONTAL, false);
                binding.rvListData.setLayoutManager(linearLayoutManager);
                binding.rvListData.setAdapter(listDataAdapter);
                Toast.makeText(getApplicationContext(), "Data Received", Toast.LENGTH_SHORT).show();
            });
            binding.btnPrint.setVisibility(View.VISIBLE);
            binding.etPrintNumber.setVisibility(View.VISIBLE);
        });

        binding.btnPrint.setOnClickListener(v -> {
            if (dataIndex == null) {
                Toast.makeText(getApplicationContext(), "Harap mengisi nomor file yang ingin diprint dengan cara memilih nomor yang tersedia", Toast.LENGTH_LONG).show();
            } else {
                bluetooth.send(print + "[" + dataIndex + "]", false);
                Toast.makeText(getApplicationContext(), "Printing running", Toast.LENGTH_SHORT).show();
            }
        });

        binding.cvSettingGps.setOnClickListener(v -> {
            binding.btnLoadSet.setVisibility(View.VISIBLE);
            Toast.makeText(getApplicationContext(), "Harap menekan tombol load set", Toast.LENGTH_SHORT).show();
        });

        binding.btnLoadSet.setOnClickListener(v -> {
            if (bluetooth.getServiceState() != -1) {
                bluetooth.send(loadSet, false);
                Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
                bluetooth.setOnDataReceivedListener((data, message) -> {
                    String receivedData = new String(data);
                    String[] bagi = receivedData.split(",");
                    binding.etNameSet.setText(checkIfArrayIsOutOfBond(bagi, 0));
                    binding.etLocationSet.setText(checkIfArrayIsOutOfBond(bagi, 1));
                    binding.etLatitudeSet.setText(checkIfArrayIsOutOfBond(bagi, 2));
                    binding.etLongitudeSet.setText(checkIfArrayIsOutOfBond(bagi, 3));
                    Toast.makeText(getApplicationContext(), "Data Received", Toast.LENGTH_SHORT).show();
                });
                binding.btnGetLocation.setVisibility(View.VISIBLE);
                binding.etNameSet.setVisibility(View.VISIBLE);
                binding.etLocationSet.setVisibility(View.VISIBLE);
                binding.etLatitudeSet.setVisibility(View.VISIBLE);
                binding.etLongitudeSet.setVisibility(View.VISIBLE);
            } else {
                checkBluetoothAvailability();
            }
        });

        binding.btnGetLocation.setOnClickListener(v -> {
            FusedLocationProviderClient mFusedLocation = LocationServices.getFusedLocationProviderClient(MainActivity.this);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplication(), "GPS Not Found. Please check permission your location apps", Toast.LENGTH_SHORT).show();
                return;
            }
            mFusedLocation.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                if (location != null) {

                    try {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        String address = addresses.get(0).getAddressLine(0);

                        Toast.makeText(MainActivity.this, "" + address, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    double lattt = location.getLatitude();
                    double lonttt = location.getLongitude();
                    String lats = String.valueOf(lattt);
                    String lons = String.valueOf(lonttt);
                    lats = lats.substring(0, lats.length() - 1);
                    lons = lons.substring(0, lons.length() - 1);
                    binding.etLatitudeSet.setText(getString(R.string.one_string_with_white_space_in_the_beginning_pattern, lats));
                    binding.etLongitudeSet.setText(getString(R.string.one_string_with_white_space_in_the_beginning_pattern, lons));

                    Toast.makeText(MainActivity.this,
                            "Lat: " + lats + " Long: " + lons,
                            Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(MainActivity.this, "Not Get Location. Please turn on your GPS smartphone", Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.btnMenu.setOnClickListener(view -> {
            if (bluetooth.getServiceState() != -1) {
                bluetooth.send(menuCommand, false);
                Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
            } else {
                checkBluetoothAvailability();
            }
        });

        binding.btnUp.setOnClickListener(view -> {
            if (bluetooth.getServiceState() != -1) {
                bluetooth.send(upCommand, false);
                Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
            } else {
                checkBluetoothAvailability();
            }
        });

        binding.btnDown.setOnClickListener(view -> {
            if (bluetooth.getServiceState() != -1) {
                bluetooth.send(downCommand, false);
                Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
            } else {
                checkBluetoothAvailability();
            }
        });

        binding.btnBack.setOnClickListener(view -> {
            if (bluetooth.getServiceState() != -1) {
                bluetooth.send(backCommand, false);
                Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
            } else {
                checkBluetoothAvailability();
            }
        });

        binding.btnEnter.setOnClickListener(view -> {
            if (bluetooth.getServiceState() != -1) {
                bluetooth.send(enterCommand, false);
                Toast.makeText(getApplicationContext(), "Data Sent", Toast.LENGTH_SHORT).show();
            } else {
                checkBluetoothAvailability();
            }
        });
    }

    private void checkBluetoothAvailability() {
        bluetooth = new BluetoothSPP(MainActivity.this);
        if (hasGrantedBluetoothPermissions()) {
            if (!bluetooth.isBluetoothAvailable()) {
                Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                if (!bluetooth.isBluetoothEnabled()) {
                    bluetooth.enable();
                } else {
                    if (!bluetooth.isServiceAvailable()) {
                        bluetooth.setupService();
                        bluetooth.startService(BluetoothState.DEVICE_OTHER);
                    }
                }
            }
        } else {
            requestForBluetoothPermissions();
        }
    }

    private String checkIfArrayIsOutOfBond(String[] dataArray, int dataIndex) {
        if (dataArray.length > dataIndex) {
            return dataArray[dataIndex];
        } else {
            return "0";
        }
    }

    private void requestLocation() {
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(5000);
        request.setFastestInterval(2000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());
        result.addOnCompleteListener(task -> {
            try {
                LocationSettingsResponse response = task.getResult(ApiException.class);

            } catch (ApiException e) {
                switch (e.getStatusCode()) {
                    case LocationSettingsStatusCodes
                            .RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(MainActivity.this, REQUEST_LOCATION)
                            ;
                        } catch (IntentSender.SendIntentException sendIntentException) {
                            sendIntentException.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    private void getLocation() {

        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }

        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .check();
    }

    public void onStart() {
        super.onStart();
    }

    public void onDestroy() {
        super.onDestroy();
        bluetooth.stopService();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bluetooth.connect(data);

        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bluetooth.setupService();
            } else {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> MainActivity.this.finish())
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void address() {
        FusedLocationProviderClient mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplication(), "GPS Not Found. Please check permission your location apps", Toast.LENGTH_SHORT).show();
            return;
        }
        mFusedLocation.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {

                try {
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    String address = addresses.get(0).getAddressLine(0);

                    Toast.makeText(getApplicationContext(), "" + address, Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                Toast.makeText(MainActivity.this, "Not Get Location. Please turn on your GPS smartphone", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_PERMISSIONS_ABOVE_29_REQUEST_CODE) {
            if (grantResults.length == BLUETOOTH_ABOVE_29_PERMISSION.length) {
                for (final int grantResult : grantResults) {
                    if (PackageManager.PERMISSION_GRANTED != grantResult) {
                        return;
                    }
                }
                if (!bluetooth.isBluetoothAvailable()) {
                    Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }

        if (requestCode == BLUETOOTH_PERMISSIONS_BELOW_30_REQUEST_CODE) {
            if (grantResults.length == BLUETOOTH_BELOW_30_PERMISSION.length) {
                for (final int grantResult : grantResults) {
                    if (PackageManager.PERMISSION_GRANTED != grantResult) {
                        return;
                    }
                }
                if (!bluetooth.isBluetoothAvailable()) {
                    Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private boolean hasGrantedBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestForBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, BLUETOOTH_BELOW_30_PERMISSION, BLUETOOTH_PERMISSIONS_BELOW_30_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, BLUETOOTH_ABOVE_29_PERMISSION, BLUETOOTH_PERMISSIONS_ABOVE_29_REQUEST_CODE);
        }
    }
}