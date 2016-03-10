import java.util.*;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;
import java.lang.Thread;
import java.util.Timer;
import java.util.TimerTask;
import org.sql2o.*;

import static spark.Spark.*;

public class App {
  public static Integer globalUserId;
  public static void main(String[] args) {
    staticFileLocation("/public");
    String layout = "templates/layout.vtl";

      TimerTask task = new TimerTask(){
        public void run(){
          if (globalUserId != null){
            Tamagotchi newTama = Tamagotchi.find(User.find(globalUserId).getTamagotchiId());
            if (newTama != null){
              System.out.println((Tamagotchi.find(User.find(globalUserId).getTamagotchiId())).getAge());
              System.out.println((Tamagotchi.find(User.find(globalUserId).getTamagotchiId())).isAlive());
              newTama.updateAge();
              newTama.isAlive();
            }
          }
        }
      };
      Timer timer = new Timer();
      long delay = 0;
      long intervalPeriod = 1000;
      timer.scheduleAtFixedRate(task, delay, intervalPeriod);
//user info & game page
    get("/", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      request.session().attribute("user", null);
      model.put("template", "templates/index.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/takeTwo", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      boolean incorrectUsername = request.session().attribute("incorrectUsername");
      boolean incorrectPassword = request.session().attribute("incorrectPassword");
      User user = request.session().attribute("user");
      model.put("incorrectUsername", incorrectUsername);
      model.put("incorrectPassword", incorrectPassword);
      model.put("user", user);
      model.put("template", "templates/index.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/signUp", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      model.put("template", "templates/sign_up.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    post("/checkPassword", (request, response) -> {
      String inputPassword = request.queryParams("password");
      String inputName = request.queryParams("name");
      User user = User.findByName(inputName);

      if(user != null) {
        if(user.getPassword().equals(inputPassword)) {
          request.session().attribute("incorrectPassword", false);
          request.session().attribute("incorrectUsername", false);
          request.session().attribute("user", user);
          globalUserId = user.getId();
          response.redirect("/games");
          return null;
        } else {
          request.session().attribute("incorrectPassword", true);
          request.session().attribute("incorrectUsername", false);
          request.session().attribute("user", user);
          response.redirect("/takeTwo");
          return null;
        }
      }
      request.session().attribute("incorrectPassword", false);
      request.session().attribute("incorrectUsername", true);
      request.session().attribute("user", null);
      response.redirect("/takeTwo");
      return null;
    });

    post("/signedUp", (request, response) -> {
      String inputPassword = request.queryParams("password");
      String inputName = request.queryParams("name");
      String inputPasswordHint = request.queryParams("passwordhint");
      if(inputName.trim().length() > 0 && inputPassword.trim().length() > 0) {
        User user = new User(inputName, inputPassword, "user");
        user.save();
        if(inputPasswordHint.length() > 0) {
          user.assignPasswordHint(inputPasswordHint);
        }
      }
      response.redirect("/");
      return null;
    });

    get("/games", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      System.out.println(globalUserId);
      User user = request.session().attribute("user");
      System.out.println(globalUserId);
      System.out.println(User.find(globalUserId).getTamagotchiId());
      model.put("user", user);
      model.put("template", "templates/games.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/profile", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      model.put("user", user);
      model.put("template", "templates/profile.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    post("/updateImage", (request, response) -> {
      String imageUrl = request.queryParams("profilePic");
      User user = request.session().attribute("user");
      user.assignPorfilepic(imageUrl);
      response.redirect("/profile");
      return null;
    });

    post("/changePassword", (request, response) -> {
      String updatedPassword = request.queryParams("updatePassword");
      User user = request.session().attribute("user");
      user.updatePassword(updatedPassword);
      response.redirect("/profile");
      return null;
    });
//simon says
    get("/simonSays", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      Turn.delete();
      User user = request.session().attribute("user");
      model.put("user", user);
      model.put("users", User.getSimonHighScores());
      model.put("template", "templates/simonSays.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    post("/next-turn", (request, response) -> {
      //score based on length of sequence, number of turns completed and difficulty multiplier
      request.session().attribute("simonScore", 0);
      Turn.resetShownStatus();
      Turn.deleteUserGuess();
      Double difficulty = Double.parseDouble(request.queryParams("difficulty")) * -1.0;
      request.session().attribute("time", difficulty);
      Double diffMultiplierDouble = 1.0 / difficulty;
      Integer diffMultiplier = diffMultiplierDouble.intValue();
      request.session().attribute("diffMultiplier", diffMultiplier);
      Turn newTurn = new Turn();
      newTurn.save();
      response.redirect("/3");
      return null;
      });

    get("/next-turn", (request, response) -> {
      Integer turns = Turn.all().size();
      Integer score = request.session().attribute("simonScore");
      Integer diffMultiplier = request.session().attribute("diffMultiplier");
      Integer addedScore = turns * diffMultiplier;
      score += addedScore;
      request.session().attribute("simonScore", score);
      Turn.resetShownStatus();
      Turn.deleteUserGuess();
      Turn newTurn = new Turn();
      newTurn.save();
      response.redirect("/replay");
      return null;
      });

      get("/3", (request, response) -> {
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("template", "templates/3.vtl");
        return new ModelAndView (model, layout);
      }, new VelocityTemplateEngine());

      get("/2", (request, response) -> {
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("template", "templates/2.vtl");
        return new ModelAndView (model, layout);
      }, new VelocityTemplateEngine());

      get("/1", (request, response) -> {
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("template", "templates/1.vtl");
        return new ModelAndView (model, layout);
      }, new VelocityTemplateEngine());

    get("/replay", (request, response) -> {
      if (Turn.allShown() == false) {
        Turn unshownTurn = Turn.getNextUnshownTurn();
        String color = unshownTurn.getGeneratedColor();
        unshownTurn.updateShownStatus();
        if(color.equals("red")) {
          response.redirect("/red");
          return null;
        } else if(color.equals("yellow")) {
          response.redirect("/yellow");
          return null;
        } else if(color.equals("green")) {
          response.redirect("/green");
          return null;
        } else if(color.equals("blue")) {
          response.redirect("/blue");
          return null;
        } else {
          response.redirect("/error-replay");
          return null;
        }
      } else {
        response.redirect("/play");
        return null;
        }
    });

    get("/yellow", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      Double time = request.session().attribute("time");
      Integer currentScore = request.session().attribute("simonScore");
      model.put("currentScore", currentScore);
      model.put("time", time);
      model.put("user", user);
      model.put("template", "templates/yellow.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/red", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      Double time = request.session().attribute("time");
      Integer currentScore = request.session().attribute("simonScore");
      model.put("currentScore", currentScore);
      model.put("time", time);
      model.put("user", user);
      model.put("template", "templates/red.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/green", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      Double time = request.session().attribute("time");
      Integer currentScore = request.session().attribute("simonScore");
      model.put("currentScore", currentScore);
      model.put("time", time);
      model.put("user", user);
      model.put("template", "templates/green.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/blue", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      Double time = request.session().attribute("time");
      Integer currentScore = request.session().attribute("simonScore");
      model.put("currentScore", currentScore);
      model.put("time", time);
      model.put("user", user);
      model.put("template", "templates/blue.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/grey", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      Double time = request.session().attribute("time");
      Integer currentScore = request.session().attribute("simonScore");
      model.put("currentScore", currentScore);
      model.put("time", time);
      model.put("user", user);
      model.put("template", "templates/grey.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/play", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      if (Turn.isFull()){
        response.redirect("/next-turn");
        return null;
      }
      User user = request.session().attribute("user");

      Integer currentScore = request.session().attribute("simonScore");
      System.out.println(currentScore);
      model.put("currentScore", currentScore);
      model.put("user", user);
      model.put("template", "templates/play.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    post("/play", (request, response) -> {
      if (!Turn.isFull()) {
        Turn currentTurn = Turn.getCurrentTurn();
        String userGuess = request.queryParams("color");
        currentTurn.update(userGuess);
        if (currentTurn.checkGuess()){
          response.redirect("/play");
          return null;
        }
        response.redirect("/gameover");
        return null;
      }
      response.redirect("/next-turn");
      return null;
    });

    get("/gameover", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      Integer currentScore = request.session().attribute("simonScore");
      Integer userHighScore = user.getSimonHighScore();
      if (currentScore > userHighScore) {
        user.updateSimonScore(currentScore);
        String congrats = "Congratulations you set a new record!";
        model.put("congrats", congrats);
      }
      userHighScore = user.getSimonHighScore();
      model.put("currentScore", currentScore);
      model.put("highScore", userHighScore);
      model.put("user", user);
      model.put("users", User.getSimonHighScores());
      model.put("template", "templates/gameover.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());
//tamagotchi
    get("/tamagotchi", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      if (User.find(globalUserId).getTamagotchiId() != 0){
        response.redirect("/newtamagotchi");
        return null;
      }
      model.put("user", user);
      model.put("template", "templates/tamagotchi.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/newtamagotchi", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      Tamagotchi tamagotchi = Tamagotchi.find(User.find(globalUserId).getTamagotchiId());
      if (!tamagotchi.isAlive()){
        User.find(globalUserId).clearTamagotchi();
      }
      model.put("tamagotchi", tamagotchi);
      model.put("user", user);
      model.put("template", "templates/newtamagotchi.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    post("/newtamagotchi", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      String name = request.queryParams("name");
      Tamagotchi tamagotchi = new Tamagotchi(name);
      tamagotchi.save();
      user.updateTamagotchi(tamagotchi.getId());
      model.put("user", user);
      model.put("tamagotchi", tamagotchi);
      model.put("template", "templates/newtamagotchi.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    post("/tamagotchiupdate/:id", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      int id = Integer.parseInt(request.params("id"));
      Tamagotchi tamagotchi = Tamagotchi.find(id);
      String action = request.queryParams("action");
      if (!tamagotchi.isAlive()){
        User.find(globalUserId).clearTamagotchi();
      }
      if (action.equals("feed")){
        tamagotchi.updateOnFeed();
        response.redirect("/feedtamagotchi/" + tamagotchi.getId());
        return null;
      } else if (action.equals("play")){
        tamagotchi.updateOnPlay();
        response.redirect("/playtamagotchi/" + tamagotchi.getId());
        return null;
      } else if (action.equals("sleep")){
        tamagotchi.updateOnSleep();
        response.redirect("/sleeptamagotchi/" + tamagotchi.getId());
        return null;
      }
      model.put("tamagotchi", tamagotchi);
      model.put("user", user);
      model.put("template", "templates/newtamagotchi.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/feedtamagotchi/:id", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      int id = Integer.parseInt(request.params("id"));
      Tamagotchi tamagotchi = Tamagotchi.find(id);
      model.put("user", user);
      model.put("tamagotchi", tamagotchi);
      model.put("user", user);
      model.put("template", "templates/feedtamagotchi.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/playtamagotchi/:id", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      int id = Integer.parseInt(request.params("id"));
      Tamagotchi tamagotchi = Tamagotchi.find(id);
      model.put("user", user);
      model.put("tamagotchi", tamagotchi);
      model.put("user", user);
      model.put("template", "templates/playtamagotchi.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/sleeptamagotchi/:id", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      int id = Integer.parseInt(request.params("id"));
      Tamagotchi tamagotchi = Tamagotchi.find(id);
      model.put("user", user);
      model.put("tamagotchi", tamagotchi);
      model.put("user", user);
      model.put("template", "templates/sleeptamagotchi.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());
//memory
    get("/memory", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      model.put("user", user);
      model.put("users", User.getMemoryHighScores());
      model.put("template", "templates/memory.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    post("/memory", (request, response) -> {
      Card.delete();
      Card.fillDatabase();
      List<Card> cards = Card.all();
      ArrayList<Card> cardDeck = new ArrayList<Card>();
      int numberOfCards = Integer.parseInt(request.queryParams("cardNumber"));
      while (cardDeck.size() < numberOfCards) {
        int number = Card.randomEvenNumber();
        if (!cardDeck.contains(cards.get(number))) {
          cardDeck.add(cards.get(number));
          cardDeck.add(cards.get(number + 1));
        }
      }
      int memoryScore = numberOfCards*10;
      request.session().attribute("memoryScore", memoryScore);
      request.session().attribute("cardNumber", request.queryParams("cardNumber"));
      Collections.shuffle(cardDeck);
      int counter = 0;
      for(Card card : cardDeck) {
        card.assignOrderId(counter);
        counter += 1;
      }
      String players = request.queryParams("players");

      request.session().attribute("cards", cardDeck);
      response.redirect("/memoryBoard");
      return null;
    });

    get("/memoryBoard", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      List<Card> cards = request.session().attribute("cards");
      int score = request.session().attribute("memoryScore");
      model.put("score", score);
      model.put("user", user);
      model.put("cards", cards);
      model.put("template", "templates/memoryBoard.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/memoryBoard", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      List<Card> cards = request.session().attribute("cards");
      model.put("user", user);
      model.put("cards", cards);
      model.put("template", "templates/memoryBoard.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    post("/memoryBoard", (request, response) -> {
      //score based on deck size, wrong guesses
      List<Card> cards = request.session().attribute("cards");
      Card card = cards.get(Integer.parseInt(request.queryParams("cards")));
      card.updateShown();
      int counter = 0;
      for(Card cardd : cards) {
        if (cardd.getShown()) {
          counter += 1;
        }
      }
      if (counter == 1) {
        request.session().attribute("firstCard", card);
      }
      if (counter == 2) {
        Card caard = request.session().attribute("firstCard");
        Card secondCard = cards.get(Integer.parseInt(request.queryParams("cards")));
        request.session().attribute("secondCard", secondCard);
        boolean cardMatch = caard.checkMatch(secondCard);
        if (cardMatch) {
          int score = request.session().attribute("memoryScore");
          score += 10;
          request.session().attribute("memoryScore", score);
          caard.matched();
          caard.updateShown();
          secondCard.matched();
          secondCard.updateShown();
          int matchedCounter = 0;
          for(Card ccard : cards) {
            if(ccard.getMatch()) {
              matchedCounter += 1;
            }
          }
          if(matchedCounter == cards.size()) {
            response.redirect("/memoryGameOver");
            return null;
          }
        } else {
          int score = request.session().attribute("memoryScore");
          score -= 5;
          request.session().attribute("memoryScore", score);
          response.redirect("/showCards");
          return null;
        }
      }
      request.session().attribute("cards", cards);
      response.redirect("/memoryBoard");
      return null;
    });

    get("/showCards", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      List<Card> cards = request.session().attribute("cards");
      int score = request.session().attribute("memoryScore");
      model.put("score", score);
      model.put("user", user);
      model.put("cards", cards);
      model.put("template", "templates/showCards.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/flipCards", (request, response) -> {
      User user = request.session().attribute("user");
      List<Card> cards = request.session().attribute("cards");
      Card firstCard = request.session().attribute("firstCard");
      firstCard.updateShown();
      Card secondCard = request.session().attribute("secondCard");
      secondCard.updateShown();
      response.redirect("/memoryBoard");
      return null;
    });

    get("/memoryGameOver", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      int score = request.session().attribute("memoryScore");
      int cardNumber = Integer.parseInt(request.session().attribute("cardNumber"));
      score += cardNumber*10;
      user.updateMemoryScore(score);
      request.session().attribute("memoryScore", score);
      model.put("score", score);
      model.put("user", user);
      model.put("users", User.getMemoryHighScores());
      model.put("template", "templates/memoryGameOver.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());
//hangman
    get("/hangman", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      model.put("user", user);
      model.put("users", User.getSimonHighScores());
      model.put("template", "templates/hangman.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

    get("/hangman", (request, response) -> {
      HashMap<String, Object> model = new HashMap<String, Object>();
      User user = request.session().attribute("user");
      model.put("user", user);
      model.put("users", User.getSimonHighScores());
      String userGuess = request.queryParams("guess");

      model.put("template", "templates/hangman.vtl");
      return new ModelAndView (model, layout);
    }, new VelocityTemplateEngine());

  } //end of main
} //end of app
