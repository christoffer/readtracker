package googlebooks;

import java.util.List;

import googlebooks.model.Volume;
import googlebooks.rx.GetBooks;
import rx.Observable;

public class GoogleBooksClient {

  public static Observable<List<Volume>> findBooks(GoogleBooksApi api, String query) {
    return Observable.create(new GetBooks(api, query));
  }
}
