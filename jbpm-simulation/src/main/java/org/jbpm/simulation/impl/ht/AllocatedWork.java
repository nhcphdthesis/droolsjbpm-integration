/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.jbpm.simulation.impl.ht;

public class AllocatedWork {

    private long allocatedTime;
    private long waitTime;
    private long duration;
    
    public AllocatedWork(long duration) {
        this.duration = duration;
    }
    public long getAllocatedTime() {
        return allocatedTime;
    }
    public void setAllocatedTime(long allocatedTime) {
        this.allocatedTime = allocatedTime;
    }
    public long getWaitTime() {
        return waitTime;
    }
    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }
    public long getDuration() {
        return duration;
    }
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public boolean isAllocated() {
    	System.out.println(String.format("IsAllocated? %s. duration+waitTime: %d, allocatedTime: %d", duration + waitTime == allocatedTime,duration+waitTime,allocatedTime));
    	//if wait time is 0, then if allocated time covers duration, then the work is succesfully allocated
    	//if wait time is not 0, if fully allocated (allocated time (==end time) is start time+duration, then wait time + duration is allocated time (end time))
    	//if wait time is not 0, if partially allocated (allocated tie = limit - start time), 
        return duration + waitTime == allocatedTime;
    }
    public void merge(AllocatedWork allocate) {
    	System.out.println(String.format("merging in AllocatedWork. current allocated time: %d, waiting time: %d, to allocate time: %d, wait time: %d", this.allocatedTime,this.waitTime,allocate.getAllocatedTime(),allocate.getWaitTime()));
       this.allocatedTime += allocate.getAllocatedTime();
       this.waitTime += allocate.getWaitTime();
        
    }
}
