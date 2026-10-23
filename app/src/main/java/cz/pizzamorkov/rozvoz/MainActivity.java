
package cz.pizzamorkov.rozvoz;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    LinearLayout layout;
    JSONArray orders;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        load();
        home();
    }

    void load() {
        try {
            orders = new JSONArray(getPreferences(0)
                .getString("orders", "[]"));
        } catch (Exception e) {
            orders = new JSONArray();
        }
    }

    void save() {
        getPreferences(0).edit()
            .putString("orders", orders.toString()).apply();
    }

    String today() {
        return new SimpleDateFormat("yyyy-MM-dd",
            Locale.getDefault()).format(new Date());
    }

    TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Color.BLACK);
        t.setPadding(12, 16, 12, 16);
        return t;
    }

    Button button(String s, Runnable action) {
        Button b = new Button(this);
        b.setText(s);
        layout.addView(b);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    void screen(String title) {
        ScrollView scroll = new ScrollView(this);
        layout = new LinearLayout(this);
        layout.setOrientation(1);
        layout.setPadding(20, 20, 20, 20);
        layout.setBackgroundColor(Color.WHITE);
        scroll.addView(layout);
        setContentView(scroll);
        layout.addView(text(title, 26));
    }

    EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        layout.addView(e);
        return e;
    }

    void home() {
        screen("Pizza Mořkov - Rozvoz");
        button("Nová objednávka", () -> newOrder());
        button("Aktivní objednávky", () -> list(false));
        button("Doručené objednávky", () -> list(true));
        button("Denní tržba a historie", () -> history());
    }

    void newOrder() {
        screen("Nová objednávka");
        EditText phone = field("Telefon zákazníka");
        EditText address = field("Adresa zákazníka");
        EditText items = field("Položky a poznámky");
        EditText price = field("Cena v Kč");
        price.setInputType(8194);

        button("Uložit objednávku", () -> {
            try {
                double amount = Double.parseDouble(
                    price.getText().toString().replace(",", "."));
                if (amount < 0) throw new Exception();

                JSONObject o = new JSONObject();
                o.put("phone", phone.getText().toString());
                o.put("address", address.getText().toString());
                o.put("items", items.getText().toString());
                o.put("price", amount);
                o.put("date", today());
                o.put("delivered", false);
                orders.put(o);
                save();
                home();
            } catch (Exception e) {
                Toast.makeText(this, "Zadej platnou cenu",
                    Toast.LENGTH_SHORT).show();
            }
        });
        button("Zpět", () -> home());
    }

    void list(boolean delivered) {
        screen(delivered ? "Doručené" : "Aktivní objednávky");
        for (int i = orders.length() - 1; i >= 0; i--) {
            JSONObject o = orders.optJSONObject(i);
            if (o == null ||
                o.optBoolean("delivered") != delivered) continue;

            final int index = i;
            layout.addView(text(
                o.optString("address") + "\n" +
                o.optString("items") + "\n" +
                o.optDouble("price") + " Kč", 18));

            button("Telefon", () -> {
                Intent intent = new Intent(Intent.ACTION_DIAL,
                    Uri.parse("tel:" + o.optString("phone")));
                startActivity(intent);
            });

            button("Navigovat - Mapy.cz", () -> {
                String url = "https://mapy.com/fnc/v1/route?" +
                    "destination=" + Uri.encode(o.optString("address")) +
                    "&navigate=true";
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(url)));
            });

            if (!delivered) {
                button("Označit jako doručené", () -> {
                    try {
                        orders.getJSONObject(index)
                            .put("delivered", true);
                        save();
                        list(false);
                    } catch (Exception e) {}
                });
            }
        }
        button("Zpět", () -> home());
    }

    void history() {
        screen("Historie tržeb");
        java.util.TreeMap<String, Double> totals =
            new java.util.TreeMap<>(
                java.util.Collections.reverseOrder());

        for (int i = 0; i < orders.length(); i++) {
            JSONObject o = orders.optJSONObject(i);
            if (o != null && o.optBoolean("delivered")) {
                String day = o.optString("date");
                totals.put(day, totals.getOrDefault(day, 0.0)
                    + o.optDouble("price"));
            }
        }

        for (String day : totals.keySet()) {
            button(day + " - " + totals.get(day) + " Kč",
                () -> dayDetail(day));
        }
        button("Zpět", () -> home());
    }

    void dayDetail(String day) {
        screen("Tržba za " + day);
        double total = 0;
        for (int i = 0; i < orders.length(); i++) {
            JSONObject o = orders.optJSONObject(i);
            if (o != null && o.optBoolean("delivered")
                && day.equals(o.optString("date"))) {
                total += o.optDouble("price");
                layout.addView(text(
                    o.optString("address") + "\n" +
                    o.optString("items") + "\n" +
                    o.optDouble("price") + " Kč", 17));
            }
        }
        layout.addView(text("Celkem: " + total + " Kč", 23));
        button("Zpět", () -> history());
    }
}
