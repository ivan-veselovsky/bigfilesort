package edu.bigfilesort;

public class TestMainCmpSort extends TestMain {

  @Override
  protected Main getMain() {
    Main m = new Main();
    m.radix = false;
    return m;
  }
  
}
