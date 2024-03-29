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

package gp.net.radius.data;


import gp.utils.arrays.Array;
import gp.utils.arrays.BitwiseXorArray;
import gp.utils.arrays.ConstantArray;
import gp.utils.arrays.DigestArray;
import gp.utils.arrays.Integer64Array;
import gp.utils.arrays.RandomArray;
import gp.utils.arrays.SupArray;

/**
 *
 * @author Gwenhael Pasquiers
 */
public class RadiusMessageUtils
{
    
    static public Array generateUniqueAuthenticator()
    {
        return new SupArray().addLast(new Integer64Array(System.currentTimeMillis())).addLast(new RandomArray(8));
    }
    
    static public Array padUserPassword(Array userPassword)
    {
        int paddingLength = ((16 - (userPassword.length % 16)) % 16);
        if(paddingLength > 0)
        {
            return new SupArray().addLast(userPassword).addLast(new ConstantArray((byte) 0, paddingLength));
        }
        else
        {
            return userPassword;
        }
    }
    
    static public Array unpadUserPassword(Array userPassword)
    {
        // remove padding
        int lastNonZero = 0;
        for(int i=0; i<userPassword.length; i++)
        {
            if(userPassword.get(i) != 0) lastNonZero++;
        }
        return userPassword.subArray(0, lastNonZero);
    }
    
    static public Array encodeUserPassword(Array authenticator, Array secret, Array userPassword)
    {
        userPassword = padUserPassword(userPassword);
        Array data = new SupArray().addLast(secret).addLast(authenticator);
        SupArray result = new SupArray();
        while(result.length < userPassword.length)
        {
            data = new BitwiseXorArray(new DigestArray(data, "MD5"), userPassword.subArray(result.length, 16));
            result.addLast(data);
        }
        return result;
    }

    static public Array decodeUserPassword(Array authenticator, Array secret, Array userPassword)
    {
        // userPassword should be an array of nx16 bytes;
        if(userPassword.length % 16 != 0)
        {
            throw new ArrayIndexOutOfBoundsException("userPassword length MUST be n*16");
        }
        
        SupArray result = new SupArray();
        int position = userPassword.length;
        
        while(position > 16)
        {
            Array data = new SupArray().addLast(secret).addLast(userPassword.subArray(position - 32, 16));
            result.addFirst(new BitwiseXorArray(new DigestArray(data, "MD5"), userPassword.subArray(position-16, 16)));
            position -= 16;
        }

        Array data = new SupArray().addLast(secret).addLast(authenticator);
        data = new BitwiseXorArray(new DigestArray(data, "MD5"), userPassword.subArray(position-16, 16));
        result.addFirst(data);
        
        return result;
    }
    
    static public String toString(RadiusMessage message)
    {
        StringBuilder stringBuidler = new StringBuilder();
        
        stringBuidler.append("[header code=").append(message.getCode()).append(", ");
        stringBuidler.append("identifier=").append(message.getIdentifier()).append(", ");
        stringBuidler.append("authenticator=").append(message.getAuthenticator()).append("]\n");
        
        for(AVPBytes avp:message.getAVPs())
        {
            stringBuidler.append("[avp code=").append(avp.getType()).append(", ");
            stringBuidler.append("length=").append(avp.getLength()).append(", ");
            stringBuidler.append("data= ").append(avp.getData().toString().replace("\n", " ")).append("]\n");
        }
        
        return stringBuidler.toString();
    }
}
