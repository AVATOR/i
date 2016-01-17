package org.igov.util;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.igov.util.convert.JSExpressionUtil;

public final class Util {

    public static final String PATTERN_DEFAULT_CONTENT_TYPE = "text/plain";
    private final static Logger LOG = LoggerFactory.getLogger(Util.class);

    public static final String DEFAULT_ENCODING = "UTF-8";

    private Util() {
    }

    public static String sCut(int nSize, String s){
        if(s!=null){
            if(s.length()>nSize){
                return new StringBuilder(s.substring(0, nSize)).append("...(+").append(s.length()-nSize).append("").append(")").toString();
            }else{
                return s;
            }
        }else{
            return null;
        }
    }
    
    public static byte[] getResourcesFile(String sRootFolder, String sPathFile) throws IOException {
        if (sPathFile.contains("..")) {
            throw new IllegalArgumentException("incorrect sPathFile!");
        }
        String sFullFileName = sRootFolder + sPathFile;
        File file = new File(sFullFileName);
        LOG.info("Loading pattern file: '{}'", sFullFileName);
        return Files.toByteArray(file);
    }

    public static String sData(byte[] a) {
        String s = "Not convertable!";
        try {
            s = new String(a, DEFAULT_ENCODING);
        } catch (Exception oException) {
            LOG.error("", oException);
        }
        return s;
    }

    public static byte[] aData(String s) {
        //log.info("[aData]:s=" + s);
        byte[] a = s.getBytes(Charset.forName(DEFAULT_ENCODING));
        //log.info("[aData]:a.length=" + a.length + ",Arrays.toString(a)=" + Arrays.toString(a));
        return a;
    }

    public static String contentByteToString(byte[] contentByte) {
        BASE64Encoder encoder = new BASE64Encoder();
        String contentString = encoder.encode(contentByte);
        return contentString;
    }

    public static byte[] contentStringToByte(String contentString) {
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] contentByte;
        try {
            contentByte = decoder.decodeBuffer(contentString);
        } catch (Exception ex) {
        	LOG.info(ex.getMessage(), ex);
            contentByte = new byte[1];
        }

        return contentByte;
    }

    public static String httpAnswer(String urlName, String sData)
            throws Exception {

        URL url = new URL(urlName);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("content-type", "application/json;charset=UTF-8");
        con.setDoOutput(true);
        DataOutputStream dos = new DataOutputStream(con.getOutputStream());
        dos.writeBytes(sData);
        dos.flush();
        dos.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                con.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String inputLine;
        while ((inputLine = br.readLine()) != null) {
            sb.append(inputLine);
        }
        br.close();
        return sb.toString();
    }




    public static String getFromFile(File file, String sCodepage) throws IOException {
        byte[] aByte = getBytesFromFile(file);
        //return Util.sData(aByte);
        //java.lang.
        if (aByte == null) {
            return null;
        }
        return new String(aByte, sCodepage == null ? DEFAULT_ENCODING : sCodepage);
        //Charset.forName(DEFAULT_ENCODING)
        //Cp1251
    }

    public static byte[] getBytesFromFile(File file) throws IOException {

        InputStream is = new FileInputStream(file);
        long length = file.length();

        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {

            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        is.close();

        return bytes;
    }


    public static String deleteContextFromURL(String URL) {
        String temp = URL.substring(URL.indexOf("//") + 2);
        return temp.substring(temp.indexOf("/"));
    }

    //return true if all args are null
    public static boolean areAllArgsNull(Object... args) {
        boolean result = true;
        for (Object o : args) {
            if (o != null) {
                result = false;
                break;
            }
        }
        return result;
    }
    
    
    public static boolean bString(String sName) {
        //LOG.info("sName",sName);
        if(sName==null || sName.length() == 0){
            return false;
        }
        //LOG.info("sName.charAt(0)",sName.charAt(0));
        if("s".equals(sName.charAt(0)+"")){//sName.startsWith("s")
            //LOG.info("(\"s\".equals={})",true);
            if (sName.length() > 1){
                //LOG.info("(sName.length() > 1={})",true);
                Character s = sName.toCharArray()[1];
                if(Character.isDigit(s)){
                    return true;
                }else if(Character.isLetter(s)){
                    //LOG.info("(Character.isLetter(s)={})",true);
                    if(Character.isUpperCase(s)){
                        //LOG.info("(Character.isUpperCase(s)={})",true);
                        return true;
                    }else{
                        return false;
                    }
                }else{
                    return false;
                }
            }else{
                return true;
            }
        }else{
            return false;
        }
    }
    
    public static String getCalculatedFormulaValue(String sFormulaOriginal, Map<String, Object> mParam) {//String
        String sReturn = null;
        String sFormula=sFormulaOriginal;
        if(sFormula==null || "".equals(sFormula.trim())){
            LOG.warn("(sFormula={},mParam(short)={})",sFormula, sCut(50, mParam.toString()));
            //oLogBig_Controller.warn("(sFormula={},mParam(short)={})",sFormula, mParam.toString());
        }else{
            for (Map.Entry<String, ?> oParam : mParam.entrySet()) {
                String sName = oParam.getKey();
                //LOG.info("sName",sName);
                if(sName != null){
                    //LOG.info("sName != null",true);
                    String sValue = oParam.getValue() == null ? "" : (String)oParam.getValue();
                    //LOG.info("sValue",sValue);
                    if(bString(sName)){
                        //LOG.info("(bString(sName)={})",true);
                        sValue = "'" + sValue + "'";
                        sFormula = sFormula.replaceAll("\\Q'["+sName+"]'\\E",sValue);
                        sFormula = sFormula.replaceAll("\\Q["+sName+"]\\E",sValue);
                    }else{
                        sFormula = sFormula.replaceAll("\\Q["+sName+"]\\E",sValue);
                    }
                    sFormula = sFormula.replaceAll("\\Q\n\\E","");
                    sFormula = sFormula.replaceAll("\\Q\r\\E","");
                }
            }
            sFormula=sFormula.substring(1);
            try{
                Map<String, Object> m = new HashMap<String, Object>();
                Object o = new JSExpressionUtil().getObjectResultOfCondition(m, mParam, sFormula); //getResultOfCondition
                sReturn = "" + o;
                LOG.info("(sFormulaOriginal={},sFormula={},mParam(cuted)={},sReturn={})",sFormulaOriginal,sFormula, sCut(50, mParam.toString()),sReturn);
                //oLogBig_Controller.info("(sFormulaOriginal={},sFormula={},mParam={},sReturn={})",sFormulaOriginal,sFormula, mParam,sReturn);
            }catch(Exception oException){
                LOG.error("FAIL: {} (sFormulaOriginal={},sFormula={},mParam(cuted)={},sReturn={})", oException.getMessage(), sFormulaOriginal, sFormula, sCut(50, mParam.toString()),sReturn);
                //oLogBig_Controller.error("FAIL: {} (sFormulaOriginal={},sFormula={},mParam(cuted)={},sReturn={})", oException.getMessage(), sFormulaOriginal, sFormula, mParam.toString(),sReturn);
            }
        }
        return sReturn;
    }

/**
     * возращает расширение файла
     * @param nameFile
     * @return
     */
    public static String getFileExp(String nameFile) {
            final Pattern oPattern = Pattern.compile("^[-a-zA-Z0-9+&#/%?=~:.;\"_*]+$");
            if (nameFile == null || nameFile.trim().isEmpty())
                    return null;
            Matcher m = oPattern.matcher(nameFile);
            if (m.find()) {
                    String exp = null;
                    for (String part : m.group(m.groupCount()).split("\\.")) {
                            exp = part;
                    }
                    return exp;
            }
            return null;
    }    

    public static boolean isTextMatched(String sWhere, String sFind) {
        return sWhere.toLowerCase().contains(sFind.toLowerCase());
    }
    
}
