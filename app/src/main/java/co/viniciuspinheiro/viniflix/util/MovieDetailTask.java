package co.viniciuspinheiro.viniflix.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import co.viniciuspinheiro.viniflix.model.Movie;
import co.viniciuspinheiro.viniflix.model.MovieDetail;

public class MovieDetailTask extends AsyncTask<String, Void, MovieDetail> {

    private WeakReference<Context> context;
    private ProgressDialog dialog;
    private MovieDetailLoader movieDetailLoader;

    public MovieDetailTask(Context context, MovieDetailLoader movieDetailLoader) {
        this.context = new WeakReference<>(context);
        this.movieDetailLoader = movieDetailLoader;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Context context = this.context.get();

        if (context != null)
            dialog = ProgressDialog.show(context, "Carregando...", "", true);
    }

    @Override
    protected MovieDetail doInBackground(String... params) {
        String url = params[0];

        try {
            URL requestUrl = new URL(url);

            HttpsURLConnection urlConnection = (HttpsURLConnection) requestUrl.openConnection();
            urlConnection.setReadTimeout(2000);
            urlConnection.setConnectTimeout(2000);

            int responseCode = urlConnection.getResponseCode();
            if (responseCode > 400) {
                throw new IOException("Erro na comunicação do servidor");
            }

            InputStream inputStream = urlConnection.getInputStream();

            BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());

            String jsonAsString = toString(in);

            MovieDetail movieDetail = getMovieDetail(new JSONObject(jsonAsString));
            in.close();

            return movieDetail;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(MovieDetail movieDetail) {
        super.onPostExecute(movieDetail);
        dialog.dismiss();

        if (movieDetail != null)
            movieDetailLoader.onResult(movieDetail);
    }
    private MovieDetail getMovieDetail(JSONObject json) throws JSONException{
        int id = json.getInt("id");
        String title = json.getString("title");
        String cast = json.getString("cast");
        String desc = json.getString("desc");
        String coverUrl = json.getString("cover_url");

        List<Movie> movies = new ArrayList<>();
        JSONArray movieArray = json.getJSONArray("movie");
        for (int i = 0; i < movieArray.length(); i++) {
            JSONObject movie = movieArray.getJSONObject(i);
            String c = movie.getString("cover_url");
            int idSimilar = movie.getInt("id");

            Movie similar = new Movie();
            similar.setId(idSimilar);
            similar.setCoverUrl(c);

            movies.add(similar);
        }
        Movie movie = new Movie();
        movie.setId(id);
        movie.setCast(cast);
        movie.setDesc(desc);
        movie.setTitle(title);
        movie.setCoverUrl(coverUrl);

        return new MovieDetail(movie, movies);
    }

    public interface MovieDetailLoader {
        void onResult(MovieDetail movieDetail);
    }
    private String toString(InputStream is) throws IOException {
        byte[] bytes = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int lidos;
        while ((lidos = is.read(bytes)) > 0) {
            baos.write(bytes, 0, lidos);
        }
        return new String(baos.toByteArray());
    }

}
