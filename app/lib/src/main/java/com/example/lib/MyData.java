package com.example.lib;



import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.URLEncoder;

class Cords {
	@SuppressWarnings("NotConstructor")
	public String[] Cords(String strona) {

		// Kompilujemy wyrażenie regularne
		String text = strona;

		// Wyrażenie regularne do dopasowania danych lng i lat
		String regex = "\"location\"\\s*:\\s*\\{\\s*\"lat\"\\s*:\\s*(-?\\d+\\.?\\d*),\\s*\"lng\"\\s*:\\s*(-?\\d+\\.?\\d*)";

		Pattern pattern = Pattern.compile(regex);

		// Tworzymy matcher do dopasowania tekstu do wyrażenia regularnego
		Matcher matcher = pattern.matcher(text);

		// Lista przechowująca pasujące wartości
		String[] cords = new String[2];

		// Przeszukujemy tekst w poszukiwaniu dopasowań
		if (matcher.find()) {
			// Grupa 1 odpowiada lng, a grupa 2 odpowiada lat
			cords[0] = matcher.group(1); // lng
			cords[1] = matcher.group(2); // lat
		}
		return cords;
	}
}

class Dane {
	@SuppressWarnings("NotConstructor")
	public String[][][] Dane() throws IOException {

		int pageNumber = 30; //liczba kart z których chcemy improtować dane
		pageNumber++;

		String[][][] dane = new String[7][10][pageNumber]; //10-liczba rekordów na strne, 7-liczba kolumn z danymi

		String link = "https://www.setlist.fm/setlists/tool-2bd6d836.html"; //https://www.concertarchives.org/bands/tool
		Scrapper1 scrapper1 = new Scrapper1(); // Tworzymy obiekt Scrapper1
		String[][] daneZeScrapper1 = scrapper1.Scrapper1(link); // Wywołujemy metodę Scrapper1

		// Przypisujemy dane z Scrapper1 do tablicy dane
		for (int i = 0; i < 10; i++) {
			for (int j=0;j<5;j++) {
				dane[j][i][0] = daneZeScrapper1[j][i];
			}

		}
		float loading= 100/pageNumber; // postep pobierania
		System.out.println("pobieranie strony: " + loading+ "%");

		String link2;
		link="https://www.setlist.fm/setlists/tool-2bd6d836.html?page=";
		for(int k = 1; k < pageNumber; k++) {
			link2=link + k; // dopisanie numeru stronu do linku
			Scrapper1 scrapper2= new Scrapper1();
			String[][] daneZeScrapper2 = scrapper1.Scrapper1(link2);
			for (int i = 0; i < 10; i++) {
				for (int j=0;j<5;j++) {
					dane[j][i][k] = daneZeScrapper2[j][i];
				}

			}

			loading= k *100/pageNumber;
			System.out.println("pobieranie danych: " + loading+ "%");
		}
		return dane;
	}
}

class ScrapLoc {
	public String[] Scraploc(String links, String title) throws IOException {
		String link = links; // Import danych z podanej strony
		URL url = new URL(link);

		// Otwieramy połączenie HTTP
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		// Odczytujemy odpowiedź
		Scanner scanner = new Scanner(connection.getInputStream());
		StringBuilder response = new StringBuilder();
		while (scanner.hasNextLine()) {
			response.append(scanner.nextLine());
		}
		scanner.close();

		// Zamykamy połączenie
		connection.disconnect();

		// Parsujemy JSON przy użyciu biblioteki Gson
		String response2=response.toString();

		Cords koord = new Cords();
		String[] kordy=koord.Cords(response2);

		return kordy;
	}
}

class Scrapper1 {
	@SuppressWarnings("NotConstructor")
	public String[][] Scrapper1(String links)throws IOException {

		String link=links; //zaimportowanie danych ze stronki
		Document doc = Jsoup.connect(link).timeout(6000).get();
		Elements body = doc.select("div.transparentBox");

		//ilosc "span month" jest taka sama jak ilosc rekordow
		String[][] data = new String[5][body.select("span.month").size()]; //finalna tablica z danymi
		String[] date = new String[body.select("span.month").size()]; //tablica z datą

		//konfiguracja daty
		int index = 0;
		for(Element e : body.select("span.day")){
			date[index]=e.text();
			index++;
		}

		index=0;
		for(Element e : body.select("span.month")){
			date[index]+= " " +e.text();
			index++;
		}

		index=0;
		for(Element e : body.select("span.year")){
			date[index]+= " " +e.text();
			index++;
		}
		// koniec konfiguracji daty

		//konfiguracja tytułu
		index=0;
		String[] title = new String[body.select("span.month").size()];
		for(Element e : body.select("a.summary")){
			title[index]=e.select("a.summary").text();
			index++;
		}
		//koniec konfiguracji tytułu

		//konfiguracja trasy koncertowej oraz lokalizacji
		index=0;
		String[] details = new String[body.select("span.month").size()];
		for(Element e : body.select("div.details")){
			details[index]=e.select("div.details").text();
			index++;
		}

		index=0;
		String[][] parts = new String[body.select("span.month").size()][4];
		for (index = 0; index < body.select("span.month").size(); index++) {
			parts[index] = details[index].split(",", 4);
		}

		//koniec dzielenia

		//utworzenie wspolnej tablicy dla wszyskich danych
		for (index = 0; index < body.select("span.month").size(); index++) {
			data[0][index]=date[index]; //date
			data[1][index]=title[index]; //title
			data[2][index]=parts[index][1]; //tour
			data[3][index]=parts[index][2]; // venue
			data[4][index]=parts[index][3]; //localization
		}
		return data;
	}
}

public class MyData {

	public static String[][][] danetab;

	public static void fillData() throws IOException{

		//pobranie danych ze stronki
		long startTime = System.currentTimeMillis();
		Dane dane = new Dane();
		danetab = dane.Dane();
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		System.out.println("Czas wykonania: " + duration + " milisekund");

		//google maps
		String apiKey = "PUT_YOUR_API_KEY_HERE"; // Replace with your Google Maps API key
		// PROSZE NIE UDOSTEPNIAC

		//dane potrzebne do połaczenia z googlem
		String cityName;
		String encodedCityName;
		String apiUrl;


		for(int k = 0; k < 30-1; k++) {
			for (int i = 0; i < 10; i++){
				cityName = danetab[4][i][k];
				encodedCityName = URLEncoder.encode(cityName, "UTF-8"); // Encode the city name for the URL
				apiUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=" + encodedCityName + "&key=" + apiKey;// Create the URL for the API request


				//link pozyskany, uruchomienie scrappera do pobrania danych z googla
				ScrapLoc location = new ScrapLoc();
				String loc[] = location.Scraploc(apiUrl,cityName);

				danetab[5][i][k]=loc[0]; //longitute
				danetab[6][i][k]=loc[1]; //latitude

//				for (int kol = 0;kol<7;kol++) {
//					System.out.print(danetab[kol][i][k] +";  ");
//				}
//				System.out.print("\n");
			}
//			System.out.print("\n");
		}

	}
	public static String[][][] getDanetab() {
		return danetab;
	}
}
