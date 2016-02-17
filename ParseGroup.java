package com.geekcrew.pkgh;


import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ParseGroup extends AsyncTask<Object, Void, Integer> {

    Activity activity;
    Ring ringShedule;

    ParseGroup(Ring ringShedule, Activity activity) {
        this.ringShedule = ringShedule;
        this.activity = activity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        System.out.println("Запуск потока поиска групп...");
    }

    @Override
    protected Integer doInBackground(Object...params) {
        List<String> ringlist = new ArrayList<>();
        Elements sheduleMatch = null;
        Elements sheduleRing = null;

        Document document = null;
        FileOutputStream stream = null;
        try {
            if (ringShedule.getDoc(activity) == null) {
                try {
                        File path = activity.getFilesDir();
                        File file = new File(path, "doc.txt");
                        Log.i("File", "(Parse Group) CREATE FILE in" + path);
                        stream = new FileOutputStream(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    document = Jsoup.connect("http://pkgh.edu.ru/obuchenie/shedule-of-classes.html").timeout(10 * 2000).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (stream != null) {
                    stream.write(document.toString().getBytes());
                    stream.close();
                } else {
                    return -1;
                }
            } else {
                Log.i("File", "(Parse Group) GET FILE");
                document = Jsoup.parse(ringShedule.getDoc(activity), "UTF-8");
            }
            sheduleMatch = document.select("div.column.one-fourth");
            sheduleRing = document.select("div.grid-box.width100.grid-v");
        } catch (OutOfMemoryError e) {
            Log.e("Большой документ", String.valueOf(e));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (sheduleRing == null || sheduleMatch == null) {
            Log.i("ELEMENTS", "sheduleRing or sheduleMatch is EMPTY");
            return -1;
        } else {
            downloadRingShedule(sheduleRing, ringlist);

            if(ringlist.isEmpty()){
                Log.i("ringlist", "EMPTY");
                return -1;
            } else {
                editRingShedule(ringlist, ringShedule.getRingWeekday(), ringShedule.getRingSaturday());
            }

            downloadNameGroup(sheduleMatch, ringShedule.getNameGroup());
            if(ringShedule.getNameGroup().isEmpty()){
                Log.i("ringShedule: ", "EMPTY");
                return -1;
            } else {
                return 0;
            }
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        ImageView imageView = (ImageView)activity.findViewById(R.id.start_pkgh);
        SweetAlertDialog alertDialog = new SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE);
        alertDialog.setTitleText("Опачки...")
                    .setContentText("Нет подключения к интернету!");
        alertDialog.getProgressHelper().setBarColor(Color.parseColor("#256db6"));

        if(result == 0) {
            imageView.setVisibility(View.INVISIBLE);
            Log.i("AsyncTask", "Done!");
        } else if (result == -1){
            imageView.setVisibility(View.INVISIBLE);
            Log.i("AsyncTask", "Result is bad!");
            alertDialog.show();
        }
    }

    private void downloadRingShedule(Elements sheduleRing, List<String> ringlist) {
            Elements tr = sheduleRing.select("tr");
            for (Element link : tr) {
                Elements td = link.select("td");
                for (Element link2 : td) {
                    if (link2.text().equals("Перерыв") || link2.text().equals("Обед") || link2.text().equals("\u00a0"))
                        break;
                    System.out.println(link2.text());
                    ringlist.add(link2.text());
                }
            }
        }
//   }

    private void downloadNameGroup(Elements sheduleMatch, List<String> nameGroup) {
        for (Element link : sheduleMatch) {
            System.out.println(link.select("h4.expanded").text());
            nameGroup.add(link.select("h4.expanded").text().toUpperCase());
        }
    }

    private void editRingShedule(List<String> ringlist, List<String> ringweekday, List<String> ringSaturday) {
        //Ring Shedule on Weekday
        for(int i = 3; i < ringlist.size(); i = i + 3) {
            String buff = ringlist.get(i);
            String begin = "";
            String end = "";
            char colon = ':';

            for (int j = 0; j < 4; j++) {
                char symBuff = buff.charAt(j);

                if (j == 2) {
                    begin += colon;
                    begin += symBuff;
                } else {
                    begin += symBuff;
                }
            }

            for (int j = buff.length() - 4; j < buff.length(); j++) {
                char symBuff = buff.charAt(j);

                if (j == buff.length() - 2) {
                    end += colon;
                    end += symBuff;
                } else {
                    end += symBuff;
                }
            }
            ringweekday.add(begin);
            ringweekday.add(end);
        }

        //Ring Shedule on Saturday
        for(int d = 4; d < ringlist.size(); d = d + 3) {
            String buffSaturay = ringlist.get(d);
            String beginSatuday = "";
            String endSaturday = "";
            char colon = ':';
            for (int j = 0; j < 4; j++) {
                char symBuffSaturday = buffSaturay.charAt(j);

                if (j == 2) {
                    beginSatuday += colon;
                    beginSatuday += symBuffSaturday;
                } else {
                    beginSatuday += symBuffSaturday;
                }
            }

            for (int j = buffSaturay.length() - 4; j < buffSaturay.length(); j++) {
                char symBuffSaturday = buffSaturay.charAt(j);

                if (j == buffSaturay.length() - 2) {
                    endSaturday += colon;
                    endSaturday += symBuffSaturday;
                } else {
                    endSaturday += symBuffSaturday;
                }
            }
            ringSaturday.add(beginSatuday);
            ringSaturday.add(endSaturday);
        }
    }
}
