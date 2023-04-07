package com.lucktracker;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.sun.jna.platform.unix.Resource;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class Monsters {
//    private HashMap<Integer, MonsterData> MONSTERTABLE = new HashMap<Integer, MonsterData>();

    Map<Integer, MonsterData> MONSTERTABLE;

    Gson gson = new Gson();

    public Monsters(){
        try {
            InputStream inputStream = this.getClass().getResourceAsStream("/processedMonsters.json");
            Reader reader = new InputStreamReader(inputStream);
            Type MONSTERDATA_TYPE = new TypeToken<List<Map<Integer, MonsterData>>>(){}.getType();
            List<Map<Integer, MonsterData>> monsterDataList = gson.fromJson(reader, MONSTERDATA_TYPE);

            MONSTERTABLE = monsterDataList.get(0);
            System.out.println("Debug hook");
        }
        catch (NullPointerException e) {
            System.out.println("ERROR READING MONSTER JSON");
        }
    }

    public MonsterData getMonsterData(int npcId) {
        return MONSTERTABLE.get(npcId);
    }
}
