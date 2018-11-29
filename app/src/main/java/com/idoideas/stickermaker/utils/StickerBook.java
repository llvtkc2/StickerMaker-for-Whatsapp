package com.idoideas.stickermaker.utils;


import android.content.Context;
import android.util.Log;

import com.idoideas.stickermaker.models.StickerPack;

import java.io.File;
import java.util.ArrayList;

public class StickerBook {

    static Context myContext;
    private static ArrayList<StickerPack> allStickerPacks = checkIfPacksAreNull();

    public static void init(Context context){
        myContext = context;
        ArrayList<StickerPack> lsp = DataArchiver.readStickerPackJSON(context);
        if(lsp!=null && lsp.size()!=0){
            allStickerPacks = lsp;
        }
    }

    private static ArrayList<StickerPack> checkIfPacksAreNull(){
        if(allStickerPacks==null){
            Log.w("IS PACKS NULL?", "YES");
            return new ArrayList<>();
        }
        Log.w("IS PACKS NULL?", "NO");
        return allStickerPacks;
    }

    public static void addStickerPackExisting(StickerPack sp){
        allStickerPacks.add(sp);
    }

    public static ArrayList<StickerPack> getAllStickerPacks(){
        return allStickerPacks;
    }

    public static StickerPack getStickerPackByName(String stickerPackName){
        for (StickerPack sp : allStickerPacks){
            if(sp.getName().equals(stickerPackName)){
                return sp;
            }
        }
        return null;
    }

    public static StickerPack getStickerPackById(String stickerPackId){
        if(allStickerPacks.isEmpty()){
            init(myContext);
        }
        Log.w("THIS IS THE ALL STICKER", allStickerPacks.toString());
        for (StickerPack sp : allStickerPacks){
            if(sp.getIdentifier().equals(stickerPackId)){
                return sp;
            }
        }
        return null;
    }

    public static StickerPack getStickerPackByIdWithContext(String stickerPackId, Context context){
        if(allStickerPacks.isEmpty()){
            init(context);
        }
        Log.w("THIS IS THE ALL STICKER", allStickerPacks.toString());
        for (StickerPack sp : allStickerPacks){
            if(sp.getIdentifier().equals(stickerPackId)){
                return sp;
            }
        }
        return null;
    }

    public static void deleteStickerPackById(String stickerPackId){
        StickerPack myStickerPack = getStickerPackById(stickerPackId);
        new File(myStickerPack.getTrayImageUri().getPath()).getParentFile().delete();
        allStickerPacks.remove(myStickerPack);
    }

    public static StickerPack getStickerPackByIndex(int index){
        return allStickerPacks.get(index);
    }
}
