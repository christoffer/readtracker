package com.readtracker.android.support;

import com.readtracker.android.db.Book;
import com.readtracker.android.db.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class DebugUtils {
  private static final Random randomGenerator = new Random();

  private static final String[] randomWords = "安憲旭 政覧属 出特退趣入 速遠使。 基室健成鳴 会張佐給選 著橋勝用 善。玲必理 育題保宣経徳 極専症改。受州 索老木 簡写 流 労器方組 。送後稼 図般景不権必長代報 島渉員危出回。上察 立 点評席実物元兵 政次搭石林商 頭助照。十辺 無 案死意後的権明武 形。外券典日由形助推様億船 供対。 発供批有 在持央覚費第 提乾廷面 彩第多 止十決 。児話社録落国 話奈協済明 面元 漲 Эним мацим мальорум ан хаж Хёз но емпэтюсъ ентэгры, фабулаз опортэры нэ эжт, ючю ты натюм клита емпэтюсъ Пошжим аппэтырэ элыктрам ку эжт, но жят жкаывола консэквюат, зймюл ютенам корпора нык эи Векж экз кэтэро конвынёры, прё эним ёнэрмйщ дэлььынётё ан Шэа ыт тота алёэнюм янтэрэсщэт Ан адхюк кэтэро ыкжпэтэндяз про Ад индоктум адвыржаряюм рыпрэхэндунт вяш, эю ыюм долорюм форынчйбюж Ку про лыгимуз майыжтатйж, квуй но кэтэро аккузата, эа ыюм видишчы фэугяат глориатюр Эа мыис аккузата ентырпрытаряш нам Эрож омнеж дольор пэр нэ, ан эчжынт чингюльищ щуавятатэ хаж Квуй видэ фэугяат ты Емпыдит молэчтёэ квуй ты, про нэ бландит котёдиэквюэ Pica maren quesset vá ríc, vá lis nirmë amilessë halyavasarya, mavor cuilë tareldar nó pel Hamba hesta ataqua árë us, ilu ná turma lindalë Ondo caima rangwë aro or Alqua orpano valarauko us tál, tul oi lasta varnë naitya An lau tulca lucië centa, é aru liquis simpetar tengwanda Winga taniquelassë nan sa, telta telpina cua oi An lië manë mornië ascarima, tó heru valda áva Vië harë cumna tellaurë nó, íta lanwa aráto lingwë cé Er nór celë hantalë, pahta palmë na oia Lië aqua pica nessa ëa, sac fárë valarauko ré Méla lorna nortalarta up mer, sondë halyavasarya órë sá Mel calpa naitya nú, mer ar limbë tengwo, be axan mavor tundo hat Inyo nauva metta áya má, vaina vacco ríc lá Vailë aiquen áya mi, fum nimba cuivië mi Ná inyo soron ataquë mer, úvë luhta pendë sindar us sausage blubber pencil cloud moon water computer school network hammer walking violently mediocre literature chair two window cords musical zebra xylophone penguin home dog final ink teacher fun website banana uncle softly mega ten awesome attatch blue internet bottle tight zone tomato prison hydro cleaning telivision send frog cup book zooming falling evily gamer lid juice moniter captain bonding loudly".split(" ");


  /**
   * Helper method that is useful for debugging. Generates a random book entry.
   *
   * @return the generated Book
   */
  public static Book generateRandomBook() {
    Book book = new Book();
    book.setTitle(getRandomWord() + " " + getRandomWord());
    book.setAuthor(getRandomWord() + " " + getRandomWord());
    if(randomGenerator.nextBoolean()) {
      book.setState(Book.State.Finished);
    } else {
      book.setState(Book.State.Reading);
    }

    if(book.getState() == Book.State.Finished) {
      String closingRemark = "";
      final int numParagrahps = randomGenerator.nextInt(10);
      for(int i = 0; i < numParagrahps; i++) {
        closingRemark += getRandomWord();
      }
      book.setClosingRemark(closingRemark);
    }

    if(randomGenerator.nextBoolean()) {
      book.setPageCount(randomGenerator.nextFloat() * 100f);
    }

    if(randomGenerator.nextBoolean()) {
      // NOTE(christoffer) These are the listed categories on lorempixel.com
      final String[] categories = new String[]{
          "abstract", "animals", "business", "cats", "city", "food", "nightlife", "fashion",
          "people", "nature", "sports", "technics", "transport",
      };
      book.setCoverImageUrl(
          String.format("http://lorempixel.com/200/200/%s/%d/%s",
              categories[randomGenerator.nextInt(categories.length)],
              randomGenerator.nextInt(10) + 1, getRandomWord()
          )
      );
    }

    if((book.getState() == Book.State.Finished) || randomGenerator.nextBoolean()) {
      book.setCurrentPosition(randomGenerator.nextFloat());

      final long DAYS_TO_MS = 1000 * 60 * 60 * 24;
      book.setCurrentPositionTimestampMs(
          System.currentTimeMillis() - randomGenerator.nextInt(30) * DAYS_TO_MS
      );

      book.setFirstPositionTimestampMs(
          book.getCurrentPositionTimestampMs() - randomGenerator.nextInt(30) * DAYS_TO_MS
      );


    }

    return book;
  }

  public static ArrayList<Session> getSessionsForBook(Book book) {
    final int numSessions = randomGenerator.nextInt(35) + 1;
    ArrayList<Session> sessions = new ArrayList<>(numSessions);
    final Float[] sessionStops = new Float[numSessions];
    for(int i = 0; i < numSessions; i++) {
      sessionStops[i] = randomGenerator.nextFloat() * book.getCurrentPosition();
    }
    Arrays.sort(sessionStops);
    float lastPos = 0.0f;
    Long runningTs = book.getFirstPositionTimestampMs();
    runningTs = runningTs == null ? 0 : runningTs;
    for(int i = 0; i < numSessions; i++) {
      Session session = new Session();
      session.setBook(book);
      session.setStartPosition(lastPos);
      session.setEndPosition(sessionStops[i]);
      session.setDurationSeconds(randomGenerator.nextInt(60 * 60));
      Long lastTs = book.getCurrentPositionTimestampMs();
      lastTs = lastTs == null ? 0 : lastTs;
      final Long ts = (long)(runningTs + randomGenerator.nextFloat() * (lastTs - runningTs));
      session.setTimestampMs(ts);
      sessions.add(session);

      lastPos = sessionStops[i];
      runningTs = ts;
    }

    return sessions;
  }


  private static String getRandomWord() {
    return randomWords[randomGenerator.nextInt(randomWords.length)];
  }
}
