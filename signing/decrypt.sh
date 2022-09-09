#!/bin/bash

ENCRYPTION_KEY=$1

if [ -n "$ENCRYPTION_KEY" ]; then
  # Encrypt the keyring file
  openssl aes-256-cbc -md sha256 -d -in keyring.gpg.aes -out keyring.gpg -k "${ENCRYPTION_KEY}"
else
  echo "ENCRYPTION_KEY should not be empty"
fi