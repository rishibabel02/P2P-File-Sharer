'use client';

import { useState } from 'react';
import FileUpload from '@/components/FileUpload';
import FileDownload from '@/components/FileDownload';
import InviteCode from '@/components/InviteCode';
import axios from 'axios';

export default function Home() {
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const [port, setPort] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<'upload' | 'download'>('upload');

  const handleReset = () => {
    setUploadedFile(null);
    setPort(null);
    setIsUploading(false);
  };

  const handleFileUpload = async (file: File) => {
    handleReset();
    setIsUploading(true);
    
    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await axios.post('/api/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      setPort(response.data.port);
      setUploadedFile(file)
    } catch (error) {
      console.error('Error uploading file:', error);
      alert('Failed to upload file. Please try again.');
      handleReset();
    }finally{
      setIsUploading(false);
    }
  };
  
  const handleDownload = async (port: number) => {
    setIsDownloading(true);
    try {
      const response = await axios.get(`/api/download/${port}`, {
        responseType: 'blob',
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      
      const headers = response.headers;
      let contentDisposition = '';
      
      for (const key in headers) {
        if (key.toLowerCase() === 'content-disposition') {
          contentDisposition = headers[key];
          break;
        }
      }
      
      let filename = 'downloaded-file';
      
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="(.+)"/);
        if (filenameMatch && filenameMatch.length === 2) {
          filename = filenameMatch[1];
        }
      }
      
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Error downloading file:', error);
      alert('Failed to download file. Please check the invite code and try again.');
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <header className="text-center mb-12">
        <h1 className="text-4xl font-bold text-blue-600 mb-2">DropFlo</h1>
        <p className="text-xl text-gray-600">P2P File Sharing</p>
      </header>
      
      <div className="bg-white rounded-lg shadow-lg p-6">
        <div className="flex border-b mb-6">
          <button
            className={`px-4 py-2 font-medium ${
              activeTab === 'upload'
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => setActiveTab('upload')}
          >
            Share a File
          </button>
          <button
            className={`px-4 py-2 font-medium ${
              activeTab === 'download'
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => setActiveTab('download')}
          >
            Receive a File
          </button>
        </div>
        
        {activeTab === 'upload' ? (
        <div>
          {port && !isUploading ? (
            <>
              <InviteCode port={port} />
              {uploadedFile && (
                <div className="mt-4 p-3 bg-gray-50 rounded-md">
                  <p className="text-sm text-gray-600">
                    Selected file: <span className="font-medium">{uploadedFile.name}</span> ({Math.round(uploadedFile.size / 1024)} KB)
                  </p>
                </div>
              )}
              <button
                onClick={handleReset}
                className="w-full mt-4 px-4 py-2 font-medium text-blue-600 border-2 border-blue-500 hover:bg-blue-50 rounded"
              >
                Share Another File
              </button>
            </>
          ) : (
            <>
              <FileUpload onFileUpload={handleFileUpload} isUploading={isUploading} />
              {isUploading && (
                <div className="mt-6 text-center">
                  <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-500 border-t-transparent"></div>
                  <p className="mt-2 text-gray-600">Uploading file...</p>
                </div>
              )}
            </>
          )}
        </div>
        ) : (
          <div>
            
            <FileDownload onDownload={handleDownload} isDownloading={isDownloading} />
            
            {isDownloading && (
              <div className="mt-6 text-center">
                <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-500 border-t-transparent"></div>
                <p className="mt-2 text-gray-600">Downloading file...</p>
              </div>
            )}
          </div>
        )}
      </div>
      
      <footer className="mt-12 text-center text-gray-500 text-sm">
        <p>DropFlo &copy; {new Date().getFullYear()} - P2P File Sharing</p>
      </footer>
    </div>
  );
}
