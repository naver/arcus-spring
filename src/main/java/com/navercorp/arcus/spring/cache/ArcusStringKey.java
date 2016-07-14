package com.navercorp.arcus.spring.cache;

/**
 * Created by iceru on 2016. 7. 11..
 */
public class ArcusStringKey {
  public static int light_hash(String str) {
    int hash = 7;
    for(int i = 0; i < str.length(); i++) {
      hash = hash * 31 + str.charAt(i);
    }
    return hash;
  }

  private String stringKey = null;
  private int hash = 0;

  public ArcusStringKey(String key, int hash) {
    this.stringKey = key;
    this.hash = hash;
  }

  public String getStringKey() {
    return stringKey;
  }

  public int getHash() {
    return hash;
  }
}
