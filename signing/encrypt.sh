#!/bin/bash

ENCRYPTION_KEY=$1

if [ -n "$ENCRYPTION_KEY" ]; then
  # Encrypt the keyring file
  openssl aes-256-cbc -md sha256 -e -in secring.gpg -out keyring.gpg.aes -k "${ENCRYPTION_KEY}"
else
  echo "ENCRYPTION_KEY should not be empty"
fi