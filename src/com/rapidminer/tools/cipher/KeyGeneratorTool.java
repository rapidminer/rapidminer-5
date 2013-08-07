/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2013 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.cipher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

/**
 * This class can be used to generate a new key and store it in the user
 * directory. Please note that existing keys will be overwritten
 * by objects of this class. That means that passwords stored with &quot;old&quot;
 * keys can no longer be decrypted.
 *
 * @author Ingo Mierswa
 */
public class KeyGeneratorTool {

    private static final String GENERATOR_TYPE = "DESede";

    private static final String KEY_FILE_NAME = "cipher.key";

    public static SecretKey createSecretKey() throws KeyGenerationException {
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance(GENERATOR_TYPE);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyGenerationException("Cannot generate key, generation algorithm not known.");
        }

        keyGenerator.init(168, new SecureRandom());

        // actual generation
        return keyGenerator.generateKey();
    }

    public static void createAndStoreKey() throws KeyGenerationException {
        if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
            //LogService.getRoot().config("Skip key generation in execution mode "+RapidMiner.getExecutionMode());
            LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.cipher.KeyGeneratorTool.skip_key_generation", RapidMiner.getExecutionMode());
            return;
        }
        // actual generation
        SecretKey key = createSecretKey();

        File keyFile = new File(FileSystemService.getUserRapidMinerDir(), KEY_FILE_NAME);
        if (!keyFile.delete()) {
            //LogService.getRoot().warning("Failed to delete old key file.");
            LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.cipher.KeyGeneratorTool.deleting_old_key_file");
        }

        byte[] rawKey = key.getEncoded();

        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(keyFile));
            out.writeInt(rawKey.length);
            out.write(rawKey);
            out.close();
        } catch (Exception e) {
            //LogService.getRoot().log(Level.WARNING, "Failed to generate key: "+e, e);
            LogService.getRoot().log(Level.WARNING,
            		I18N.getMessage(LogService.getRoot().getResourceBundle(),
            				"com.rapidminer.tools.cipher.KeyGeneratorTool.generating_key_error",
            				e),
            				e);
            throw new KeyGenerationException("Cannot store key: " + e.getMessage());
        }
    }

    public static Key getUserKey() throws IOException {
        File keyFile = new File(FileSystemService.getUserRapidMinerDir(), KEY_FILE_NAME);
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(keyFile));
            int length = in.readInt();
            byte[] rawKey = new byte[length];
            int actualLength = in.read(rawKey);
            if (length != actualLength)
                throw new IOException("Cannot read key file (unexpected length)");
            return makeKey(rawKey);
        } catch (Exception e) {
            throw new IOException("Cannot retrieve key: " + e.getMessage());
        } finally {
            if (in != null)
                in.close();
        }
    }

    public static SecretKeySpec makeKey(byte[] rawKey) {
        return new SecretKeySpec(rawKey, GENERATOR_TYPE);
    }
}
