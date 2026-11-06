package ru.morcool.sender;

import java.io.*;import java.util.*;import java.util.zip.*;
import javax.xml.parsers.*;import org.w3c.dom.*;

/** Small dependency-free XLSX reader: shared strings, inline strings, workbook sheet relationships. */
public final class Sheets {
    public static class Book {public final List<String> names=new ArrayList<>();public final List<List<List<String>>> sheets=new ArrayList<>();}
    static Document xml(byte[] b)throws Exception{
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);
        try{f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);}catch(Exception ignored){}
        f.setExpandEntityReferences(false);String s=new String(b,"UTF-8");if(s.toUpperCase(Locale.ROOT).contains("<!DOCTYPE"))throw new IOException("DOCTYPE запрещён");
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(b));
    }
    static NodeList all(Element e,String name){return e.getElementsByTagNameNS("*",name);}
    static String content(Element e,String name){NodeList n=all(e,name);StringBuilder b=new StringBuilder();for(int i=0;i<n.getLength();i++)b.append(n.item(i).getTextContent());return b.toString();}
    static int col(String ref){int n=0;for(char c:ref.toCharArray()){if(c<'A'||c>'Z')break;n=n*26+c-'A'+1;}return n-1;}
    public static Book read(byte[] bytes)throws Exception {
        Map<String,byte[]> files=new HashMap<>();long size=0;
        try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;while((e=z.getNextEntry())!=null){if(e.isDirectory())continue;ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=z.read(b))!=-1){size+=n;if(size>32*1024*1024)throw new IOException("Распакованный Excel больше 32 МБ");o.write(b,0,n);}files.put(e.getName(),o.toByteArray());if(files.size()>4096)throw new IOException("Слишком много частей Excel");}}
        if(!files.containsKey("xl/workbook.xml"))throw new IOException("Нужен файл Excel .xlsx (не .xls)");
        List<String> shared=new ArrayList<>();if(files.containsKey("xl/sharedStrings.xml")){NodeList s=xml(files.get("xl/sharedStrings.xml")).getElementsByTagNameNS("*","si");for(int i=0;i<s.getLength();i++)shared.add(content((Element)s.item(i),"t"));}
        Map<String,String> rels=new HashMap<>();NodeList rel=xml(files.get("xl/_rels/workbook.xml.rels")).getElementsByTagNameNS("*","Relationship");
        for(int i=0;i<rel.getLength();i++){Element e=(Element)rel.item(i);String t=e.getAttribute("Target");rels.put(e.getAttribute("Id"),t.startsWith("/")?t.substring(1):"xl/"+t);}
        Book out=new Book();NodeList sheets=xml(files.get("xl/workbook.xml")).getElementsByTagNameNS("*","sheet");
        for(int i=0;i<sheets.getLength();i++){
            Element sh=(Element)sheets.item(i);String path=rels.get(sh.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships","id"));if(!files.containsKey(path))continue;
            List<List<String>> rows=new ArrayList<>();NodeList rn=xml(files.get(path)).getElementsByTagNameNS("*","row");
            if(rn.getLength()>100000)throw new IOException("Слишком много строк Excel");
            for(int r=0;r<rn.getLength();r++){List<String> row=new ArrayList<>();NodeList cn=all((Element)rn.item(r),"c");for(int j=0;j<cn.getLength();j++){
                Element ce=(Element)cn.item(j);int idx=col(ce.getAttribute("r"));if(idx<0)idx=row.size();if(idx>1023)throw new IOException("Больше 1024 колонок");while(row.size()<=idx)row.add("");
                String v=content(ce,"v"),t=ce.getAttribute("t");if(t.equals("s"))v=shared.get(Integer.parseInt(v));else if(t.equals("inlineStr"))v=content(ce,"t");
                else if(v.matches("[0-9.]+[Ee][+]?[0-9]+"))v=new java.math.BigDecimal(v).toPlainString();
                row.set(idx,v);
            }if(!row.isEmpty())rows.add(row);}
            out.names.add(sh.getAttribute("name"));out.sheets.add(rows);
        }if(out.sheets.isEmpty())throw new IOException("В книге нет доступных листов");return out;
    }
}
