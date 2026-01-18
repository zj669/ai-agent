#!/usr/bin/env python3
"""
Docker Image Manager - Handle Docker image operations

This script manages Docker images including:
- Building images from Dockerfiles
- Pulling images from registries
- Pushing images to registries
- Listing and cleaning images
- Image inspection and analysis
"""

import argparse
import subprocess
import json
import sys
import os
import re
from datetime import datetime
from typing import Dict, List, Optional

class DockerImageManager:
    def __init__(self):
        self.docker_available = self._check_docker_availability()
        
    def _check_docker_availability(self) -> bool:
        """Check if Docker is available"""
        try:
            result = subprocess.run(['docker', 'version'], 
                                  capture_output=True, text=True, timeout=10)
            return result.returncode == 0
        except (subprocess.TimeoutExpired, FileNotFoundError):
            return False
    
    def _run_docker_command(self, command: List[str], capture_output: bool = True) -> tuple:
        """Execute Docker command with proper encoding handling"""
        if not self.docker_available:
            return 1, "", "Docker is not available"
            
        try:
            if capture_output:
                # Use bytes mode and manual UTF-8 decoding to avoid Windows encoding issues
                result = subprocess.run(['docker'] + command, 
                                      capture_output=True, timeout=120)
                stdout = result.stdout.decode('utf-8', errors='replace') if result.stdout else ""
                stderr = result.stderr.decode('utf-8', errors='replace') if result.stderr else ""
                return result.returncode, stdout, stderr
            else:
                result = subprocess.run(['docker'] + command, timeout=120)
                return result.returncode, "", ""
        except subprocess.TimeoutExpired:
            return 1, "", f"Command timed out: docker {' '.join(command)}"
        except Exception as e:
            return 1, "", f"Error executing command: {str(e)}"
    
    def list_images(self, all_images: bool = False) -> Dict:
        """List Docker images"""
        cmd = ['images']
        if all_images:
            cmd.append('-a')
        cmd.extend(['--format', 'json'])
        
        code, stdout, stderr = self._run_docker_command(cmd)
        if code != 0:
            # Fallback to table format
            cmd = ['images']
            if all_images:
                cmd.append('-a')
            code, stdout, stderr = self._run_docker_command(cmd)
            if code == 0:
                return {"images": self._parse_images_table(stdout), "format": "table"}
            else:
                return {"error": stderr or "Failed to list images"}
        
        try:
            images = [json.loads(line) for line in stdout.strip().split('\n') if line]
            return {"images": images, "format": "json"}
        except json.JSONDecodeError:
            return {"images": [], "format": "raw", "raw_output": stdout}
    
    def _parse_images_table(self, output: str) -> List[Dict]:
        """Parse docker images table output"""
        lines = output.strip().split('\n')
        if len(lines) < 2:
            return []
        
        # Skip header line and parse data
        images = []
        for line in lines[1:]:
            if line.strip():
                parts = line.split()
                if len(parts) >= 3:
                    image_info = {
                        'repository': parts[0],
                        'tag': parts[1],
                        'image_id': parts[2],
                        'created': parts[3] if len(parts) > 3 else '',
                        'size': parts[4] if len(parts) > 4 else ''
                    }
                    images.append(image_info)
        
        return images
    
    def build_image(self, context_path: str, tag: str = None, dockerfile: str = 'Dockerfile',
                   no_cache: bool = False, pull: bool = False) -> Dict:
        """Build Docker image"""
        cmd = ['build']
        if tag:
            cmd.extend(['-t', tag])
        if no_cache:
            cmd.append('--no-cache')
        if pull:
            cmd.append('--pull')
        cmd.extend(['-f', dockerfile, context_path])
        
        code, stdout, stderr = self._run_docker_command(cmd, capture_output=False)
        if code == 0:
            return {"success": True, "message": f"Image built successfully{f' with tag {tag}' if tag else ''}"}
        else:
            return {"success": False, "error": stderr or "Failed to build image"}
    
    def pull_image(self, image_name: str) -> Dict:
        """Pull image from registry"""
        code, stdout, stderr = self._run_docker_command(['pull', image_name], capture_output=False)
        if code == 0:
            return {"success": True, "message": f"Image {image_name} pulled successfully"}
        else:
            return {"success": False, "error": stderr or "Failed to pull image"}
    
    def push_image(self, image_name: str) -> Dict:
        """Push image to registry"""
        code, stdout, stderr = self._run_docker_command(['push', image_name], capture_output=False)
        if code == 0:
            return {"success": True, "message": f"Image {image_name} pushed successfully"}
        else:
            return {"success": False, "error": stderr or "Failed to push image"}
    
    def remove_image(self, image_name: str, force: bool = False) -> Dict:
        """Remove Docker image"""
        cmd = ['rmi']
        if force:
            cmd.append('-f')
        cmd.append(image_name)
        
        code, stdout, stderr = self._run_docker_command(cmd)
        if code == 0:
            return {"success": True, "message": f"Image {image_name} removed successfully"}
        else:
            return {"success": False, "error": stderr or "Failed to remove image"}
    
    def inspect_image(self, image_name: str) -> Dict:
        """Inspect Docker image details"""
        code, stdout, stderr = self._run_docker_command(['inspect', image_name])
        if code == 0:
            try:
                data = json.loads(stdout)
                return {"success": True, "inspection": data}
            except json.JSONDecodeError:
                return {"success": False, "error": "Invalid JSON response from Docker"}
        else:
            return {"success": False, "error": stderr or "Failed to inspect image"}
    
    def prune_images(self, all_images: bool = False, force: bool = False) -> Dict:
        """Remove unused images"""
        cmd = ['image', 'prune']
        if all_images:
            cmd.append('-a')
        if force:
            cmd.append('-f')
        else:
            cmd.append('--filter')
            cmd.append('until=24h')
            
        code, stdout, stderr = self._run_docker_command(cmd)
        if code == 0:
            return {"success": True, "message": "Unused images pruned successfully", "output": stdout}
        else:
            return {"success": False, "error": stderr or "Failed to prune images"}
    
    def tag_image(self, source_image: str, target_image: str) -> Dict:
        """Tag an image with a new name"""
        code, stdout, stderr = self._run_docker_command(['tag', source_image, target_image])
        if code == 0:
            return {"success": True, "message": f"Image tagged from {source_image} to {target_image}"}
        else:
            return {"success": False, "error": stderr or "Failed to tag image"}

def main():
    parser = argparse.ArgumentParser(description='Docker Image Manager')
    parser.add_argument('--action', required=True,
                       choices=['list', 'build', 'pull', 'push', 'remove', 'inspect', 
                               'prune', 'tag'],
                       help='Action to perform')
    parser.add_argument('--image', help='Image name')
    parser.add_argument('--tag', help='Image tag')
    parser.add_argument('--context', help='Build context path')
    parser.add_argument('--dockerfile', default='Dockerfile', help='Dockerfile path')
    parser.add_argument('--all', action='store_true', help='Show all images/list all for prune')
    parser.add_argument('--force', action='store_true', help='Force operation')
    parser.add_argument('--no-cache', action='store_true', help='Build without cache')
    parser.add_argument('--pull', action='store_true', help='Pull base images during build')
    parser.add_argument('--source-image', help='Source image for tagging')
    parser.add_argument('--target-image', help='Target image name for tagging')
    
    args = parser.parse_args()
    
    manager = DockerImageManager()
    
    if args.action == 'list':
        result = manager.list_images(args.all)
    elif args.action == 'build' and args.context:
        result = manager.build_image(args.context, args.tag, args.dockerfile, 
                                   args.no_cache, args.pull)
    elif args.action == 'pull' and args.image:
        result = manager.pull_image(args.image)
    elif args.action == 'push' and args.image:
        result = manager.push_image(args.image)
    elif args.action == 'remove' and args.image:
        result = manager.remove_image(args.image, args.force)
    elif args.action == 'inspect' and args.image:
        result = manager.inspect_image(args.image)
    elif args.action == 'prune':
        result = manager.prune_images(args.all, args.force)
    elif args.action == 'tag' and args.source_image and args.target_image:
        result = manager.tag_image(args.source_image, args.target_image)
    else:
        result = {"error": "Invalid arguments or missing required parameters"}
    
    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    main()