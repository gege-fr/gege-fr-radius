/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package gp.net.radius.dictionary;

import gp.utils.map.AssociationHashMapUniquenessException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

/**
 *
 * @author Gwenhael Pasquiers
 */
public class RadiusDictionary
{
    private RadiusCodes radiusCodes;
    private RadiusVendors radiusVendors;
    private final String defaultVendorName = "Base";
    private final Integer defaultVendorCode = -1;

    public RadiusDictionary(File file) throws FileNotFoundException, IOException, AssociationHashMapUniquenessException
    {
        this.radiusCodes = new RadiusCodes();
        this.radiusVendors = new RadiusVendors();
        this.radiusVendors.addVendor(this.defaultVendorName, this.defaultVendorCode);
        
        this.parseFile(file);
    }

    public RadiusCodes getRadiusCodes()
    {
        return this.radiusCodes;
    }
    
    public RadiusVendors getRadiusVendors()
    {
        return this.radiusVendors;
    }

    public RadiusAttributes getRadiusAttributes(Integer vendorCode)
    {
        return this.radiusVendors.getRadiusAttributes(vendorCode);
    }
    
    public RadiusValues getRadiusValues(Integer vendorCode, Integer attributeCode)
    {
        RadiusAttributes radiusAttributes = this.radiusVendors.getRadiusAttributes(vendorCode);
        
        if(null != radiusAttributes)
        {
            return radiusAttributes.getAttributeRadiusValues(vendorCode);
        }
        else
        {
            return null;
        }
    }
    
    private void parseFile(File file) throws FileNotFoundException, IOException, AssociationHashMapUniquenessException
    {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        String line;

        String currentVendorName = this.defaultVendorName;
        Integer currentVendorCode = this.defaultVendorCode;

        LinkedList<File> includes = new LinkedList<File>();

        while (null != (line = bufferedReader.readLine()))
        {
            if ((line.length() == 0) || line.startsWith("#"))
            {
                continue;
            }

            line = this.cleanLine(line);

            if (line.toUpperCase().startsWith("$INCLUDE"))
            {
                includes.addLast(new File(file.getParent() + File.separator + line.split(" ")[1]));
            }
            else if (line.toUpperCase().startsWith("END-VENDOR"))
            {
                currentVendorName = this.defaultVendorName;
                currentVendorCode = this.defaultVendorCode;
            }
            else if (line.toUpperCase().startsWith("VENDOR"))
            {
                String[] splitted = line.split(" ");
                currentVendorName = splitted[1];
                currentVendorCode = Integer.valueOf(splitted[2]);
                this.radiusVendors.addVendor(currentVendorName, currentVendorCode);

            }
            else if (line.toUpperCase().startsWith("ATTRIBUTE"))
            {
                String[] splitted = line.split(" ");
                this.radiusVendors.getRadiusAttributes(currentVendorCode).addAttribute(splitted[1], Integer.valueOf(splitted[2]), splitted[3]);
            }
            else if (line.toUpperCase().startsWith("VALUE"))
            {
                String[] splitted = line.split(" ");
                Integer currentAttributeCode = this.radiusVendors.getRadiusAttributes(currentVendorCode).getAttributeCode(splitted[1]);

                Integer vendorCodeToUse;

                if (null == currentAttributeCode)
                {
                    vendorCodeToUse = new Integer(-1);
                    currentAttributeCode = this.radiusVendors.getRadiusAttributes(vendorCodeToUse).getAttributeCode(splitted[1]);
                }
                else
                {
                    vendorCodeToUse = currentVendorCode;
                }

                Integer currentValueValue;

                if (splitted[3].startsWith("0x"))
                {
                    currentValueValue = Integer.valueOf(splitted[3].substring(2), 16);
                }
                else
                {
                    currentValueValue = Integer.valueOf(splitted[3]);
                }

                this.radiusVendors.getRadiusAttributes(vendorCodeToUse).getAttributeRadiusValues(currentAttributeCode).addValue(splitted[2], currentValueValue);
            }
        }

        bufferedReader.close();

        for (File include : includes)
        {
            this.parseFile(include);
        }
    }

    static public void main(String... args)
    {
        try
        {
            RadiusDictionary dic = new RadiusDictionary(new File("./res/radius/dictionary"));
            Integer vcode = dic.getRadiusVendors().getVendorCode("Ascend");
            Integer acode = dic.getRadiusVendors().getRadiusAttributes(vcode).getAttributeCode("Ascend-Service-Type");
            Integer value = dic.getRadiusVendors().getRadiusAttributes(vcode).getAttributeRadiusValues(acode).getValueCode("Ascend-Service-Type-NetToNet");
            System.out.println(value);
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.out);
        }
    }

    private String cleanLine(String line)
    {
        line = line.trim().replace("\t", " ");

        String newLine = line.replace("  ", " ");

        while (line.length() != newLine.length())
        {
            line = newLine;
            newLine = line.replace("  ", " ");
        }

        return line;
    }
}
