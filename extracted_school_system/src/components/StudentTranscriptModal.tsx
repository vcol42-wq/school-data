import React from 'react';
import { Student } from '../types';
import { StudentGradeProfileModal } from './grades/StudentGradeProfileModal';

interface StudentTranscriptModalProps {
  student: Student;
  isOpen: boolean;
  onClose: () => void;
  subjectsList?: string[];
  schoolName?: string;
}

export const StudentTranscriptModal: React.FC<StudentTranscriptModalProps> = ({ 
  student, 
  isOpen, 
  onClose,
  subjectsList,
  schoolName
}) => {
  const defaultSubjects = subjectsList || [
    'التربية الإسلامية',
    'اللغة العربية',
    'اللغة الإنكليزية',
    'الرياضيات',
    'الاجتماعيات',
    'الكيمياء',
    'الأحياء',
    'الفيزياء',
    'النشاط البدني',
    'التربية الفنية',
    'التربية الأخلاقية',
    'الحاسوب'
  ];

  return (
    <StudentGradeProfileModal
      student={student}
      isOpen={isOpen}
      onClose={onClose}
      subjectsList={defaultSubjects}
      schoolName={schoolName}
    />
  );
};
